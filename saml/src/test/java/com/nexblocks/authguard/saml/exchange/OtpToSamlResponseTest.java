package com.nexblocks.authguard.saml.exchange;

import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.saml.ImmutableAuthnClassRefs;
import com.nexblocks.authguard.saml.ImmutableSamlAuthnRequest;
import com.nexblocks.authguard.saml.SamlAuthnRequest;
import com.nexblocks.authguard.saml.SamlService;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableSamlSignatures;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.StatusCode;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OtpToSamlResponseTest {
    private static final String KEYSTORE_PASSWORD = "test";
    private static final String KEY_ALIAS = "idp-signing";
    private static String keystorePath;

    private OtpToSamlResponse exchange;

    @BeforeAll
    static void setUpClass() {
        SamlService.initOpenSAML();

        URL keystoreUrl = SamlResponseProviderTest.class.getClassLoader()
                .getResource("idp-signing.p12");
        assertNotNull(keystoreUrl, "Test keystore resource not found");
        keystorePath = keystoreUrl.getPath();
    }

    @BeforeEach
    void setupTest() {
        OtpVerifier otpVerifier = Mockito.mock(OtpVerifier.class);
        AccountsService accountsService = Mockito.mock(AccountsService.class);

        Mockito.when(otpVerifier.verifyAccountTokenAsync(Mockito.any()))
                .thenReturn(Uni.createFrom().item(1L));
        Mockito.when(accountsService.getById(1L, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(AccountBO.builder().build())));

        SamlConfiguration configuration = ImmutableSamlConfiguration.builder()
                .issuer("test")
                .signature(ImmutableSamlSignatures.builder()
                        .useKeyStore(true)
                        .privateKey("private")
                        .publicKey("public")
                        .keyStorePath(keystorePath)
                        .keyStoreKeyAlias(KEY_ALIAS)
                        .keyStorePassword(KEYSTORE_PASSWORD)
                        .signAssertion(true)
                        .signResponse(false)
                        .build())
                .build();

        exchange = new OtpToSamlResponse(otpVerifier, accountsService, configuration);
    }

    @Test
    void exchangeMatchingSamlCondition() {
        SamlAuthnRequest samlRequest = ImmutableSamlAuthnRequest.builder()
                .authnClassRefs(ImmutableAuthnClassRefs.builder()
                        .addClassRefs(AuthnContext.PASSWORD_AUTHN_CTX)
                        .build())
                .build();
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .domain("main")
                .extraParameters(samlRequest)
                .build();

        AuthResponseBO response = exchange.exchange(authRequest).subscribeAsCompletionStage().join();

        String token = (String) response.getToken();
        String decodedXml = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);

        assertThat(decodedXml).isNotNull();
        assertThat(decodedXml).doesNotContain(StatusCode.AUTHN_FAILED);
    }

    @Test
    void exchangeNotMatchingSamlCondition() {
        SamlAuthnRequest samlRequest = ImmutableSamlAuthnRequest.builder()
                .authnClassRefs(ImmutableAuthnClassRefs.builder()
                        .addClassRefs(AuthnContext.SMARTCARD_AUTHN_CTX)
                        .build())
                .build();
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .domain("main")
                .extraParameters(samlRequest)
                .build();

        AuthResponseBO response = exchange.exchange(authRequest).subscribeAsCompletionStage().join();

        String token = (String) response.getToken();
        String decodedXml = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);

        assertThat(decodedXml).isNotNull();
        assertThat(decodedXml).contains(StatusCode.AUTHN_FAILED);
    }
}