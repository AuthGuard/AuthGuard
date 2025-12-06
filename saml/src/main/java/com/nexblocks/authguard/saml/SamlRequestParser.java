package com.nexblocks.authguard.saml;

import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.saml.signatures.SamlSigner;
import io.vavr.control.Either;
import org.opensaml.saml.saml2.core.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class SamlRequestParser {
    public static final String SAML2P_NS = "urn:oasis:names:tc:SAML:2.0:protocol";
    public static final String SAML2_NS  = "urn:oasis:names:tc:SAML:2.0:assertion";

    public enum Binding { REDIRECT, POST }

    private final SamlSigner signer;
    private final SamlConfiguration configuration;

    public SamlRequestParser(final SamlConfiguration samlConfiguration) {
        this.signer = new SamlSigner(samlConfiguration);
        this.configuration = samlConfiguration;
    }

    public Either<SamlErrorResponse, SamlAuthnRequest> parseAuthnRequest(final Binding binding,
                                                                         final String samlRequest,
                                                                         final String relayState) {
        String xml = switch (binding) {
            case POST -> new String(base64Decode(samlRequest), StandardCharsets.UTF_8);
            case REDIRECT -> decodeRedirectSamlRequest(samlRequest);
        };

        Document doc = XmlRequestParser.secureParse(xml);

        Element authnRequest = XmlRequestParser.getFirstElement(doc, new QName(SAML2P_NS, "AuthnRequest"));

        if (authnRequest == null) {
            return Either.left(SamlErrorResponse.nonServiceProviderError(new RequestValidationError(Collections.singletonList(
                    new Violation("AuthnRequest", ViolationType.INVALID_VALUE)
            ))));
        };

        // required stuff
        String requestId       = XmlRequestParser.getAttribute(authnRequest, "ID");
        String acsUrl          = XmlRequestParser.getAttribute(authnRequest, "AssertionConsumerServiceURL");

        Element issuerEl = XmlRequestParser.getFirstChild(authnRequest, new QName(SAML2_NS, "Issuer"));
        String issuer    = issuerEl != null ? issuerEl.getTextContent().trim() : null;

        if (acsUrl == null) {
            return Either.left(SamlErrorResponse.nonServiceProviderError(new RequestValidationError(Collections.singletonList(
                    new Violation("AuthnRequest.acsUrl", ViolationType.MISSING_REQUIRED_VALUE)
            ))));
        }

        if (requestId == null) {
            Response response = SamlErrorResponseProvider.requestDenied(
                    configuration.getIssuer(), acsUrl, "", "Missing AuthnRequest.ID");

            return createSignedError(response, acsUrl);
        }

        if (issuer == null) {
            Response response = SamlErrorResponseProvider.requestDenied(
                    configuration.getIssuer(), acsUrl, "", "Missing AuthnRequest.Issuer");

            return Either.left(SamlErrorResponse.serviceProviderError(response, acsUrl));
        }

        // optional stuff
        Boolean forceAuthn     = XmlRequestParser.boolBooleanAttribute(authnRequest, "ForceAuthn");
        Boolean isPassive      = XmlRequestParser.boolBooleanAttribute(authnRequest, "IsPassive");

        SamlAuthnRequest.ProtocolBinding protocolBinding = parseProtocolBinding(authnRequest);

        if (protocolBinding != SamlAuthnRequest.ProtocolBinding.HTTP_POST) {
            Response response = SamlErrorResponseProvider.unsupportedBinding("", acsUrl, requestId);

            return createSignedError(response, acsUrl);
        }

        Either<RequestValidationError, SamlAuthnRequest.NameIdFormat> nameIdFormats = parseNameIdFormat(authnRequest);

        if (nameIdFormats.isLeft()) {
            Response response = SamlErrorResponseProvider.invalidNameIDPolicy("", acsUrl, requestId);

            return createSignedError(response, acsUrl);
        }

        SamlAuthnRequest.AuthnClassRefs authnContext = parseClassRefs(authnRequest);

        // TODO should we keep this? Should we keep the same value?
        if (relayState != null && relayState.length() > 4096) {
            Response response = SamlErrorResponseProvider.requestDenied(
                    configuration.getIssuer(), acsUrl, requestId, "RelayState is too large");

            return createSignedError(response, acsUrl);
        }

        return Either.right(ImmutableSamlAuthnRequest.builder()
                .requestId(requestId)
                .issuer(issuer)
                .acsUrl(acsUrl)
                .protocolBinding(protocolBinding)
                .forceAuthn(forceAuthn != null && forceAuthn)
                .passive(isPassive != null && isPassive)
                .relayState(relayState)
                .authnClassRefs(authnContext)
                .nameIdFormat(nameIdFormats.get())
                .build());
    }

    private Either<SamlErrorResponse, SamlAuthnRequest> createSignedError(final Response response,
                                                                          final String acsUrl) {
        signer.signSamlError(response);

        return Either.left(SamlErrorResponse.serviceProviderError(response, acsUrl));
    }

    private static SamlAuthnRequest.ProtocolBinding parseProtocolBinding(final Element authnRequest) {
        String protocolBinding = XmlRequestParser.getAttribute(authnRequest, "ProtocolBinding");

        if (protocolBinding == null) {
            return SamlAuthnRequest.ProtocolBinding.HTTP_POST;
        }

        if (protocolBinding.equals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST")) {
            return SamlAuthnRequest.ProtocolBinding.HTTP_POST;
        }

        return SamlAuthnRequest.ProtocolBinding.OTHER;
    }

    private static SamlAuthnRequest.AuthnClassRefs parseClassRefs(final Element authnRequest) {
        Element rac = XmlRequestParser.getFirstChild(authnRequest, new QName(SAML2P_NS, "RequestedAuthnContext"));

        if (rac == null) {
            return ImmutableAuthnClassRefs.builder()
                    .unset(true)
                    .build();
        }

        String racComparison = XmlRequestParser.getAttribute(rac, "Comparison");

        List<String> classRefs = new ArrayList<>();

        NodeList refs = rac.getElementsByTagNameNS(SAML2_NS, "AuthnContextClassRef");

        for (int i = 0; i < refs.getLength(); i++) {
            String classRefValue = refs.item(i).getTextContent();

            if (classRefValue != null && !classRefValue.isBlank()) {
                classRefs.add(classRefValue.trim());
            }
        }

        return ImmutableAuthnClassRefs.builder()
                .comparison(racComparison == null ? "exact" : "")
                .classRefs(classRefs)
                .build();
    }

    private static Either<RequestValidationError, SamlAuthnRequest.NameIdFormat> parseNameIdFormat(final Element authnRequest) {
        Element policy = XmlRequestParser.getFirstChild(authnRequest, new QName(SAML2P_NS, "NameIDPolicy"));

        if (policy == null) {
            return Either.right(SamlAuthnRequest.NameIdFormat.UNSPECIFIED);
        }

        String nameIdFormat = XmlRequestParser.getAttribute(policy, "Format");

        if (nameIdFormat == null) {
            return Either.right(SamlAuthnRequest.NameIdFormat.UNSPECIFIED);
        }

        return switch (nameIdFormat) {
            case "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent" ->
                    Either.right(SamlAuthnRequest.NameIdFormat.PERSISTENT);
            case "urn:oasis:names:tc:SAML:2.0:nameid-format:transient" ->
                    Either.right(SamlAuthnRequest.NameIdFormat.TRANSIENT);
            case "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress" ->
                    Either.right(SamlAuthnRequest.NameIdFormat.EMAIL_ADDRESS);
            case "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified" ->
                    Either.right(SamlAuthnRequest.NameIdFormat.UNSPECIFIED);
            default -> Either.left(new RequestValidationError(Collections.singletonList(
                    new Violation("NameIDPolicy", ViolationType.INVALID_VALUE)
            )));
        };
    }

    /** Redirect binding decoding: (maybe) URL-decode → Base64-decode → raw DEFLATE inflate */
    private static String decodeRedirectSamlRequest(final String samlRequestParam) {
        String maybeDecoded = samlRequestParam;
        // TODO this might not be needed, verify whether the values is already decoded
        if (samlRequestParam.contains("%")) {
            maybeDecoded = URLDecoder.decode(samlRequestParam, StandardCharsets.UTF_8);
        }

        byte[] compressed = base64Decode(maybeDecoded);
        byte[] inflated = inflateRawDeflate(compressed);

        return new String(inflated, StandardCharsets.UTF_8);
    }

    private static byte[] base64Decode(final String s) {
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("SAMLRequest is not valid Base64", e);
        }
    }

    private static byte[] inflateRawDeflate(final byte[] data) {
        try (var bin = new ByteArrayInputStream(data);
             var in = new InflaterInputStream(bin, new Inflater(true))) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to inflate SAMLRequest (Redirect binding)", e);
        }
    }
}
