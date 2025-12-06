package com.nexblocks.authguard.saml;

import com.nexblocks.authguard.saml.SamlRequestParser.Binding;
import com.nexblocks.authguard.saml.config.ImmutableAttributesConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableMfaConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableSamlSignatures;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.saml.exchange.SigningSupport;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class SamlRequestParserTest {
    private static String keystorePath;

    @BeforeAll
    static void beforeAll() {
        SamlService.initOpenSAML();

        URL keystoreUrl = SamlRequestParserTest.class.getClassLoader().getResource("idp-signing.p12");
        assert keystoreUrl != null : "Test keystore resource not found";
        keystorePath = keystoreUrl.getPath();
    }

    private SamlConfiguration config() {
        return ImmutableSamlConfiguration.builder()
                .issuer("urn:test-idp")
                .useUsername(false)
                .useEmail(false)
                .usePhoneNumber(false)
                .csrfTokenKey("csrf")
                .responseAttributes(ImmutableAttributesConfiguration.builder()
                        .includeUsername(false)
                        .includeEmail(false)
                        .includePhoneNumber(false)
                        .build())
                .useMultiFactorAuthentication(ImmutableMfaConfiguration.builder()
                        .useOtp(false)
                        .build())
                .signature(ImmutableSamlSignatures.builder()
                        .useKeyStore(true)
                        .keyStorePath(keystorePath)
                        .keyStoreKeyAlias("idp-signing")
                        .keyStorePassword("test")
                        .privateKey("private")
                        .publicKey("public")
                        .build())
                .build();
    }

    private static String authnXml(final String id, final String acs, final String issuer, final String binding,
                                   final String nameIdFormat, final boolean includeRac) {
        StringBuilder sb = new StringBuilder();
        sb.append("<saml2p:AuthnRequest xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ")
                .append("ID=\"").append(id).append("\" ")
                .append("Version=\"2.0\" ")
                .append("AssertionConsumerServiceURL=\"").append(acs).append("\" ")
                .append("ProtocolBinding=\"").append(binding).append("\">");
        if (issuer != null) {
            sb.append("<saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">")
                    .append(issuer)
                    .append("</saml2:Issuer>");
        }

        if (nameIdFormat != null) {
            sb.append("<saml2p:NameIDPolicy Format=\"")
                    .append(nameIdFormat)
                    .append("\"/>");
        }

        if (includeRac) {
            sb.append("<saml2p:RequestedAuthnContext>")
                    .append("<saml2:AuthnContextClassRef xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>")
                    .append("</saml2p:RequestedAuthnContext>");
        }

        sb.append("</saml2p:AuthnRequest>");
        return sb.toString();
    }

    private static String encodePost(final String xml) {
        return Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeRedirect(final String xml) {
        try {
            byte[] input = xml.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true); // 'true' => nowrap (raw DEFLATE)
            try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater)) {
                dos.write(input);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parseAuthnRequest_post_success() {
        SamlRequestParser parser = new SamlRequestParser(config());
        String xml = authnXml("_id-1", "https://sp.example.com/acs", "urn:sp", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",
                "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", true);
        String samlRequest = encodePost(xml);

        Either<SamlErrorResponse, SamlAuthnRequest> result = parser.parseAuthnRequest(Binding.POST, samlRequest, "relay");

        assertThat(result.isRight()).isTrue();
        SamlAuthnRequest req = result.get();
        assertThat(req.getRequestId()).isEqualTo("_id-1");
        assertThat(req.getIssuer()).isEqualTo("urn:sp");
        assertThat(req.getAcsUrl()).isEqualTo("https://sp.example.com/acs");
        assertThat(req.getProtocolBinding()).isEqualTo(SamlAuthnRequest.ProtocolBinding.HTTP_POST);
        assertThat(req.getRelayState()).isEqualTo("relay");
        assertThat(req.getNameIdFormat()).isEqualTo(SamlAuthnRequest.NameIdFormat.EMAIL_ADDRESS);
        assertThat(req.getAuthnClassRefs().isUnset()).isFalse();
    }

    @Test
    void parseAuthnRequest_redirect_success() {
        SamlRequestParser parser = new SamlRequestParser(config());
        String xml = authnXml("_id-2", "https://sp.example.com/acs", "urn:sp", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",
                null, false);
        String samlRequest = encodeRedirect(xml);

        Either<SamlErrorResponse, SamlAuthnRequest> result = parser.parseAuthnRequest(Binding.REDIRECT, samlRequest, "state");

        assertThat(result.isRight()).isTrue();
        SamlAuthnRequest req = result.get();
        assertThat(req.getRequestId()).isEqualTo("_id-2");
        assertThat(req.getIssuer()).isEqualTo("urn:sp");
        assertThat(req.getAcsUrl()).isEqualTo("https://sp.example.com/acs");
        assertThat(req.getRelayState()).isEqualTo("state");
    }

    @Test
    void parseAuthnRequest_missingAuthnRequest_returnsLeft() {
        SamlRequestParser parser = new SamlRequestParser(config());
        String xml = "<root></root>";
        String samlRequest = encodePost(xml);

        Either<SamlErrorResponse, SamlAuthnRequest> result = parser.parseAuthnRequest(Binding.POST, samlRequest, null);

        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void parseAuthnRequest_missingAcsUrl_returnsLeft() {
        SamlRequestParser parser = new SamlRequestParser(config());
        // Missing AssertionConsumerServiceURL
        String xml = "<saml2p:AuthnRequest xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"_id\">" +
                "<saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">urn:sp</saml2:Issuer>" +
                "</saml2p:AuthnRequest>";
        String samlRequest = encodePost(xml);

        Either<SamlErrorResponse, SamlAuthnRequest> result = parser.parseAuthnRequest(Binding.POST, samlRequest, null);

        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void parseAuthnRequest_unsupportedBinding_returnsLeft() {
        SamlRequestParser parser = new SamlRequestParser(config());
        String xml = authnXml("_id-3", "https://sp.example.com/acs", "urn:sp", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect",
                null, false);
        String samlRequest = encodePost(xml);

        Either<SamlErrorResponse, SamlAuthnRequest> result = parser.parseAuthnRequest(Binding.POST, samlRequest, null);

        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void parseAuthnRequest_invalidNameIdPolicy_returnsLeft() {
        SamlRequestParser parser = new SamlRequestParser(config());
        String xml = authnXml("_id-4", "https://sp.example.com/acs", "urn:sp", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",
                "urn:oasis:names:tc:SAML:9.9:nameid-format:unknown", false);
        String samlRequest = encodePost(xml);

        Either<SamlErrorResponse, SamlAuthnRequest> result = parser.parseAuthnRequest(Binding.POST, samlRequest, null);

        assertThat(result.isLeft()).isTrue();
    }
}