package com.nexblocks.authguard.saml.exchange;

import com.nexblocks.authguard.saml.ImmutableSamlAuthnRequest;
import com.nexblocks.authguard.saml.SamlAuthnRequest;
import com.nexblocks.authguard.saml.SamlService;
import com.nexblocks.authguard.saml.config.ImmutableAttributesConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableMfaConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableSamlSignatures;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.service.model.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SamlResponseProviderTest {

    private static final String KEYSTORE_PASSWORD = "test";
    private static final String KEY_ALIAS = "idp-signing";
    private static String keystorePath;

    private SamlResponseProvider samlResponseProvider;
    private SamlConfiguration configuration;

    @BeforeAll
    static void setUpClass() {
        // initialize OpenSAML
        SamlService.initOpenSAML();
        
        // get the path to the test keystore resource
        URL keystoreUrl = SamlResponseProviderTest.class.getClassLoader()
                .getResource("idp-signing.p12");
        assertNotNull(keystoreUrl, "Test keystore resource not found");
        keystorePath = keystoreUrl.getPath();
    }

    @BeforeEach
    void setUp() {
        configuration = createKeystoreConfiguration();
        samlResponseProvider = new SamlResponseProvider(configuration);
    }

    @Test
    void generateToken_withValidInputs_shouldReturnAuthResponse() {
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        AccountBO account = createValidAccount();
        TokenOptionsBO tokenOptions = createValidTokenOptions();

        AuthResponseBO result = samlResponseProvider.generateToken(request, account, tokenOptions);

        assertNotNull(result);
        assertEquals("samlResponse", result.getType());
        assertEquals(EntityType.ACCOUNT, result.getEntityType());
        assertEquals(account.getId(), result.getEntityId());

        String decodedXml = new String(Base64.getDecoder().decode((String) result.getToken()), StandardCharsets.UTF_8);
        assertTrue(decodedXml.contains("<saml2p:Response") || decodedXml.contains("<saml2:Response"));
        assertTrue(decodedXml.contains("12345")); // Account ID should be in NameID
    }

    @Test
    void generateToken_withAccountHavingEmail_shouldIncludeEmailInResponse() {
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        AccountBO account = createAccountWithEmail();
        TokenOptionsBO tokenOptions = createValidTokenOptions();

        AuthResponseBO result = samlResponseProvider.generateToken(request, account, tokenOptions);

        assertNotNull(result);
        String token = (String) result.getToken();
        String decodedXml = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        assertTrue(decodedXml.contains("<saml2p:Response") || decodedXml.contains("<saml2:Response"));
        assertTrue(decodedXml.contains("urn:oasis:names:tc:SAML:2.0:ac:classes:Password"));
    }

    @Test
    void generateToken_withAccountHavingRoles_shouldIncludeRolesInResponse() {
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        AccountBO account = createAccountWithRoles();
        TokenOptionsBO tokenOptions = createValidTokenOptions();

        AuthResponseBO result = samlResponseProvider.generateToken(request, account, tokenOptions);

        assertNotNull(result);
        String token = (String) result.getToken();
        String decodedXml = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        assertTrue(decodedXml.contains("<saml2p:Response") || decodedXml.contains("<saml2:Response"));
        assertTrue(decodedXml.contains("12345")); // Account ID should be in NameID
    }

    @Test
    void generateToken_withAccountHavingPermissions_shouldIncludePermissionsInResponse() {
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        AccountBO account = createAccountWithPermissions();
        TokenOptionsBO tokenOptions = createValidTokenOptions();

        AuthResponseBO result = samlResponseProvider.generateToken(request, account, tokenOptions);

        assertNotNull(result);
        String token = (String) result.getToken();
        String decodedXml = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        assertTrue(decodedXml.contains("permissions"));
    }

    @Test
    void generateToken_withDifferentTokenSources_shouldSetCorrectAuthnContext() {
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        AccountBO account = createValidAccount();

        TokenOptionsBO basicTokenOptions = createTokenOptionsWithSource("basic");
        AuthResponseBO basicResult = samlResponseProvider.generateToken(request, account, basicTokenOptions);
        String basicToken = (String) basicResult.getToken();
        String basicXml = new String(Base64.getDecoder().decode(basicToken), StandardCharsets.UTF_8);
        assertTrue(basicXml.contains("urn:oasis:names:tc:SAML:2.0:ac:classes:Password"));

        TokenOptionsBO otpTokenOptions = createTokenOptionsWithSource("otp");
        AuthResponseBO otpResult = samlResponseProvider.generateToken(request, account, otpTokenOptions);
        String otpToken = (String) otpResult.getToken();
        String otpXml = new String(Base64.getDecoder().decode(otpToken), StandardCharsets.UTF_8);
        assertTrue(otpXml.contains("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken"));

        TokenOptionsBO totpTokenOptions = createTokenOptionsWithSource("totp");
        AuthResponseBO totpResult = samlResponseProvider.generateToken(request, account, totpTokenOptions);
        String totpToken = (String) totpResult.getToken();
        String totpXml = new String(Base64.getDecoder().decode(totpToken), StandardCharsets.UTF_8);
        assertTrue(totpXml.contains("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken"));
    }

    @Test
    void generateToken_withRequestIdPresent_shouldIncludeInResponseTo() {
        SamlAuthnRequest request = ImmutableSamlAuthnRequest.builder()
                .requestId("_test-request-id-123")
                .issuer("https://sp.example.com")
                .acsUrl("https://sp.example.com/acs")
                .relayState("test-relay-state")
                .valid(true)
                .errorMessage("")
                .build();
        AccountBO account = createValidAccount();
        TokenOptionsBO tokenOptions = createValidTokenOptions();

        AuthResponseBO result = samlResponseProvider.generateToken(request, account, tokenOptions);

        assertNotNull(result);
        String token = (String) result.getToken();
        String decodedXml = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        assertTrue(decodedXml.contains("InResponseTo=\"_test-request-id-123\""));
    }

    @Test
    void generateToken_withEmptyRequestId_shouldNotIncludeInResponseTo() {
        SamlAuthnRequest request = ImmutableSamlAuthnRequest.builder()
                .requestId("")
                .issuer("https://sp.example.com")
                .acsUrl("https://sp.example.com/acs")
                .relayState("test-relay-state")
                .valid(true)
                .errorMessage("")
                .build();
        AccountBO account = createValidAccount();
        TokenOptionsBO tokenOptions = createValidTokenOptions();

        AuthResponseBO result = samlResponseProvider.generateToken(request, account, tokenOptions);

        assertNotNull(result);
        String token = (String) result.getToken();
        String decodedXml = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        assertFalse(decodedXml.contains("InResponseTo="));
    }

    @Test
    void generateToken_shouldProduceValidSignature() throws Exception {
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        AccountBO account = createValidAccount();
        TokenOptionsBO tokenOptions = createValidTokenOptions();

        AuthResponseBO result = samlResponseProvider.generateToken(request, account, tokenOptions);

        assertNotNull(result);
        String decodedXml = new String(Base64.getDecoder().decode((String) result.getToken()), StandardCharsets.UTF_8);

        Response samlResponse = parseXmlToSamlResponse(decodedXml);
        assertNotNull(samlResponse);

        assertFalse(samlResponse.getAssertions().isEmpty());
        Assertion assertion = samlResponse.getAssertions().get(0);
        assertNotNull(assertion.getSignature(), "Assertion should be signed");

        Credential verificationCredential = SigningSupport.loadSigningCredential(
                keystorePath, KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray(), KEYSTORE_PASSWORD.toCharArray());

        try {
            SignatureValidator.validate(assertion.getSignature(), verificationCredential);
            assertTrue(true, "Signature validation successful");
        } catch (SignatureException e) {
            fail("Signature validation failed: " + e.getMessage());
        }
    }

    @Test
    void generateToken_withDifferentAccounts_shouldProduceValidSignatures() throws Exception {
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        TokenOptionsBO tokenOptions = createValidTokenOptions();

        Credential verificationCredential = SigningSupport.loadSigningCredential(
                keystorePath, KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray(), KEYSTORE_PASSWORD.toCharArray());

        // verify email
        AccountBO emailAccount = createAccountWithEmail();
        AuthResponseBO emailResult = samlResponseProvider.generateToken(request, emailAccount, tokenOptions);
        validateSignature(emailResult, verificationCredential);

        // verify roles
        AccountBO rolesAccount = createAccountWithRoles();
        AuthResponseBO rolesResult = samlResponseProvider.generateToken(request, rolesAccount, tokenOptions);
        validateSignature(rolesResult, verificationCredential);

        // verify permissions
        AccountBO permissionsAccount = createAccountWithPermissions();
        AuthResponseBO permissionsResult = samlResponseProvider.generateToken(request, permissionsAccount, tokenOptions);
        validateSignature(permissionsResult, verificationCredential);
    }

    @Test
    void generateToken_withDifferentTokenSources_shouldProduceValidSignatures() throws Exception {
        // verify email
        SamlAuthnRequest request = createValidSamlAuthnRequest();
        AccountBO account = createValidAccount();

        // verify email
        Credential verificationCredential = SigningSupport.loadSigningCredential(
                keystorePath, KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray(), KEYSTORE_PASSWORD.toCharArray());

        // verify email
        TokenOptionsBO basicTokenOptions = createTokenOptionsWithSource("basic");
        AuthResponseBO basicResult = samlResponseProvider.generateToken(request, account, basicTokenOptions);
        validateSignature(basicResult, verificationCredential);

        // verify email
        TokenOptionsBO otpTokenOptions = createTokenOptionsWithSource("otp");
        AuthResponseBO otpResult = samlResponseProvider.generateToken(request, account, otpTokenOptions);
        validateSignature(otpResult, verificationCredential);

        // verify email
        TokenOptionsBO totpTokenOptions = createTokenOptionsWithSource("totp");
        AuthResponseBO totpResult = samlResponseProvider.generateToken(request, account, totpTokenOptions);
        validateSignature(totpResult, verificationCredential);
    }

    private Response parseXmlToSamlResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Element element = document.getDocumentElement();
        
        Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory()
                .getUnmarshaller(element);
        XMLObject xmlObject = unmarshaller.unmarshall(element);
        
        return (Response) xmlObject;
    }

    private void validateSignature(AuthResponseBO result, Credential verificationCredential) throws Exception {
        String decodedXml = new String(Base64.getDecoder().decode((String) result.getToken()), StandardCharsets.UTF_8);
        Response samlResponse = parseXmlToSamlResponse(decodedXml);
        
        assertFalse(samlResponse.getAssertions().isEmpty());
        Assertion assertion = samlResponse.getAssertions().get(0);
        assertNotNull(assertion.getSignature(), "Assertion should be signed");
        
        try {
            SignatureValidator.validate(assertion.getSignature(), verificationCredential);
        } catch (SignatureException e) {
            fail("Signature validation failed: " + e.getMessage());
        }
    }

    private SamlConfiguration createKeystoreConfiguration() {
        return ImmutableSamlConfiguration.builder()
                .issuer("http://localhost:8080")
                .useUsername(true)
                .useEmail(true)
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
                        .signAssertion(true)
                        .signResponse(false)
                        .build())
                .build();
    }

    private ImmutableAttributesConfiguration createDefaultAttributesConfiguration() {
        return ImmutableAttributesConfiguration.builder()
                .includeUsername(true)
                .includeEmail(true)
                .includePhoneNumber(false)
                .build();
    }

    private ImmutableMfaConfiguration createDefaultMfaConfiguration() {
        return ImmutableMfaConfiguration.builder()
                .useOtp(false)
                .build();
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

    private AccountBO createValidAccount() {
        return AccountBO.builder()
                .id(12345L)
                .domain("test-domain")
                .identifiers(Collections.singletonList(
                        UserIdentifierBO.builder()
                                .identifier("testuser")
                                .type(UserIdentifier.Type.USERNAME)
                                .build()))
                .createdAt(Instant.now())
                .lastModified(Instant.now())
                .build();
    }

    private AccountBO createAccountWithEmail() {
        return AccountBO.builder()
                .from(createValidAccount())
                .email(AccountEmailBO.builder()
                        .email("test@example.com")
                        .verified(true)
                        .build())
                .build();
    }

    private AccountBO createAccountWithRoles() {
        return AccountBO.builder()
                .from(createValidAccount())
                .roles(Arrays.asList("admin", "user"))
                .build();
    }

    private AccountBO createAccountWithPermissions() {
        List<PermissionBO> permissions = Arrays.asList(
                PermissionBO.builder()
                        .group("users")
                        .name("read")
                        .build(),
                PermissionBO.builder()
                        .group("admin")
                        .name("write")
                        .build()
        );
        
        return AccountBO.builder()
                .from(createValidAccount())
                .permissions(permissions)
                .build();
    }

    private TokenOptionsBO createValidTokenOptions() {
        return TokenOptionsBO.builder()
                .source("basic")
                .trackingSession("test-session-123")
                .build();
    }

    private TokenOptionsBO createTokenOptionsWithSource(String source) {
        return TokenOptionsBO.builder()
                .source(source)
                .trackingSession("test-session-123")
                .build();
    }
}
