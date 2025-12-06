package com.nexblocks.authguard.saml;

import com.nexblocks.authguard.saml.exchange.SamlResponseProvider;
import com.nexblocks.authguard.saml.exchange.XmlSamlResponseBuilder;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class SamlResponseMarshallerTest {
    @Test
    void xmlToString_withValidXMLObject_shouldReturnXMLString() {
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        Response response = XmlSamlResponseBuilder.createResponse(request, "http://localhost:8080", DateTime.now());

        String result = SamlResponseMarshaller.xmlToString(response);

        assertNotNull(result);
        assertTrue(result.contains("<saml2p:Response") || result.contains("<saml2:Response"));
        assertTrue(result.contains("xmlns:saml2"));
    }

    @Test
    void xmlToString_withNullXMLObject_shouldThrowRuntimeException() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> SamlResponseMarshaller.xmlToString(null));
        assertNotNull(exception.getMessage());
    }

    private SamlAuthnRequest createValidSamlAuthnRequest() {
        return ImmutableSamlAuthnRequest.builder()
                .requestId("_test-request-id")
                .issuer("https://sp.example.com")
                .acsUrl("https://sp.example.com/acs")
                .relayState("test-relay-state")
                .valid(true)
                .errorMessage("")
                .build();
    }
}