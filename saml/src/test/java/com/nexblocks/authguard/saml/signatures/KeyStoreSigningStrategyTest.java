package com.nexblocks.authguard.saml.signatures;

import com.nexblocks.authguard.saml.SamlService;
import com.nexblocks.authguard.saml.config.ImmutableAttributesConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableMfaConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableSamlSignatures;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.saml.exchange.SigningSupport;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensaml.saml.common.SignableSAMLObject;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreSigningStrategyTest {

    private static final String KEYSTORE_PASSWORD = "test";
    private static final String KEY_ALIAS = "idp-signing";
    private static String keystorePath;

    @Mock
    private SignableSAMLObject mockSignableSAMLObject;

    @BeforeAll
    static void setUpClass() {
        SamlService.initOpenSAML();

        URL keystoreUrl = KeyStoreSigningStrategyTest.class.getClassLoader()
                .getResource("idp-signing.p12");
        assertNotNull(keystoreUrl, "Test keystore resource not found");
        keystorePath = keystoreUrl.getPath();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void constructor_withValidConfiguration_shouldCreateInstance() {
        SamlConfiguration configuration = createValidConfiguration();

        assertDoesNotThrow(() -> new KeyStoreSigningStrategy(configuration));
    }

    @Test
    void constructor_withNullSignatureConfiguration_shouldThrowConfigurationException() {
        SamlConfiguration configuration = ImmutableSamlConfiguration.builder()
                .useUsername(true)
                .useEmail(false)
                .usePhoneNumber(false)
                .csrfTokenKey("csrf")
                .responseAttributes(createDefaultAttributesConfiguration())
                .useMultiFactorAuthentication(createDefaultMfaConfiguration())
                .build();

        ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> new KeyStoreSigningStrategy(configuration));
        assertEquals("Missing SAML signature configuration", exception.getMessage());
    }

    @Test
    void constructor_withNullKeyStorePath_shouldThrowConfigurationException() {
        SamlConfiguration configuration = ImmutableSamlConfiguration.builder()
                .useUsername(true)
                .useEmail(false)
                .usePhoneNumber(false)
                .csrfTokenKey("csrf")
                .responseAttributes(createDefaultAttributesConfiguration())
                .useMultiFactorAuthentication(createDefaultMfaConfiguration())
                .signature(ImmutableSamlSignatures.builder()
                        .useKeyStore(true)
                        .privateKey("private")
                        .publicKey("public")
                        .keyStoreKeyAlias(KEY_ALIAS)
                        .keyStorePassword(KEYSTORE_PASSWORD)
                        .build())
                .build();

        ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> new KeyStoreSigningStrategy(configuration));
        assertEquals("KeyStore SAML signature requires a keystore path to be included", exception.getMessage());
    }

    @Test
    void constructor_withInvalidKeyStorePath_shouldThrowConfigurationException() {
        SamlConfiguration configuration = ImmutableSamlConfiguration.builder()
                .useUsername(true)
                .useEmail(false)
                .usePhoneNumber(false)
                .csrfTokenKey("csrf")
                .responseAttributes(createDefaultAttributesConfiguration())
                .useMultiFactorAuthentication(createDefaultMfaConfiguration())
                .signature(ImmutableSamlSignatures.builder()
                        .useKeyStore(true)
                        .privateKey("private")
                        .publicKey("public")
                        .keyStorePath("/invalid/path/to/keystore.p12")
                        .keyStoreKeyAlias(KEY_ALIAS)
                        .keyStorePassword(KEYSTORE_PASSWORD)
                        .build())
                .build();

        ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> new KeyStoreSigningStrategy(configuration));
        assertEquals("Failed to read SAML signature keys", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void constructor_withInvalidKeyStorePassword_shouldThrowConfigurationException() {
        SamlConfiguration configuration = ImmutableSamlConfiguration.builder()
                .useUsername(true)
                .useEmail(false)
                .usePhoneNumber(false)
                .csrfTokenKey("csrf")
                .responseAttributes(createDefaultAttributesConfiguration())
                .useMultiFactorAuthentication(createDefaultMfaConfiguration())
                .signature(ImmutableSamlSignatures.builder()
                        .useKeyStore(true)
                        .privateKey("private")
                        .publicKey("public")
                        .keyStorePath(keystorePath)
                        .keyStoreKeyAlias(KEY_ALIAS)
                        .keyStorePassword("wrongpassword")
                        .build())
                .build();

        ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> new KeyStoreSigningStrategy(configuration));
        assertEquals("Failed to read SAML signature keys", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void constructor_withInvalidKeyAlias_shouldThrowConfigurationException() {
        SamlConfiguration configuration = ImmutableSamlConfiguration.builder()
                .useUsername(true)
                .useEmail(false)
                .usePhoneNumber(false)
                .csrfTokenKey("csrf")
                .responseAttributes(createDefaultAttributesConfiguration())
                .useMultiFactorAuthentication(createDefaultMfaConfiguration())
                .signature(ImmutableSamlSignatures.builder()
                        .useKeyStore(true)
                        .privateKey("private")
                        .publicKey("public")
                        .keyStorePath(keystorePath)
                        .keyStoreKeyAlias("invalidalias")
                        .keyStorePassword(KEYSTORE_PASSWORD)
                        .build())
                .build();

        ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> new KeyStoreSigningStrategy(configuration));
        assertEquals("Failed to read SAML signature keys", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void signSamlObject_withValidInstance_shouldCallSigningSupport() {
        SamlConfiguration configuration = createValidConfiguration();
        KeyStoreSigningStrategy strategy = new KeyStoreSigningStrategy(configuration);

        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> strategy.signSamlObject(mockSignableSAMLObject));
        assertEquals("Signing failed", exception.getMessage());
    }

    @Test
    void signSamlObject_withNullSignable_shouldThrowException() {
        SamlConfiguration configuration = createValidConfiguration();
        KeyStoreSigningStrategy strategy = new KeyStoreSigningStrategy(configuration);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> strategy.signSamlObject(null));
        assertEquals("Signing failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof NullPointerException);
    }

    private SamlConfiguration createValidConfiguration() {
        return ImmutableSamlConfiguration.builder()
                .useUsername(true)
                .useEmail(false)
                .usePhoneNumber(false)
                .csrfTokenKey("csrf")
                .responseAttributes(createDefaultAttributesConfiguration())
                .useMultiFactorAuthentication(createDefaultMfaConfiguration())
                .signature(ImmutableSamlSignatures.builder()
                        .useKeyStore(true)
                        .privateKey("private")
                        .publicKey("public")
                        .keyStorePath(keystorePath)
                        .keyStoreKeyAlias(KEY_ALIAS)
                        .keyStorePassword(KEYSTORE_PASSWORD)
                        .build())
                .build();
    }

    private ImmutableAttributesConfiguration createDefaultAttributesConfiguration() {
        return ImmutableAttributesConfiguration.builder()
                .includeUsername(true)
                .includeEmail(false)
                .includePhoneNumber(false)
                .build();
    }

    private ImmutableMfaConfiguration createDefaultMfaConfiguration() {
        return ImmutableMfaConfiguration.builder()
                .useOtp(false)
                .build();
    }
}
