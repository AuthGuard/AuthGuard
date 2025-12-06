package com.nexblocks.authguard.saml.exchange;

import com.nexblocks.authguard.saml.SamlAuthnRequest;
import com.nexblocks.authguard.saml.SamlResponseMarshaller;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.saml.signatures.SamlSigner;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;

public class SamlResponseProvider {
    private final SamlSigner samlSigner;
    private final SamlConfiguration configuration;

    public SamlResponseProvider(final SamlConfiguration configuration) {
        this.samlSigner = new SamlSigner(configuration);
        this.configuration = configuration;
    }

    public AuthResponseBO generateToken(final SamlAuthnRequest request,
                                        final AccountBO account,
                                        final TokenOptionsBO tokenOptions) {
        DateTime now = DateTime.now(DateTimeZone.UTC);

        Assertion assertion = XmlSamlResponseBuilder.createAssertion(request, account, tokenOptions, configuration.getIssuer(), now);
        Response response = XmlSamlResponseBuilder.createResponse(request, configuration.getIssuer(), now);

        samlSigner.signSamlObject(assertion, response); // signature is done in place

        response.getAssertions().add(assertion);

        String base64Response = SamlResponseMarshaller.toBase64XmlString(response);

        return AuthResponseBO.builder()
                .token(base64Response)
                .type("samlResponse")
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();
    }

    public AuthResponseBO generateError(final SamlAuthnRequest request,
                                        final Response response) {
        samlSigner.signSamlError(response); // signature is done in place

        String base64Response = SamlResponseMarshaller.toBase64XmlString(response);

        return AuthResponseBO.builder()
                .token(base64Response)
                .type("samlResponse")
                .build();
    }
}
