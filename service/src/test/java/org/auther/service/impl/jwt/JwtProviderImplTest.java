package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.auther.config.ConfigContext;
import org.auther.service.JtiProvider;
import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;
import org.auther.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtProviderImplTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "this secret is only for testing purposes";
    private static final String ISSUER = "test";

    private ConfigContext configContext;
    private JtiProvider jtiProvider;
    private JwtProviderImpl jwtProvider;

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private void basicConfig() {
        Mockito.when(configContext.getAsString("algorithm")).thenReturn(ALGORITHM);
        Mockito.when(configContext.getAsString("key")).thenReturn(KEY);
        Mockito.when(configContext.getAsString("issuer")).thenReturn(ISSUER);
        Mockito.when(configContext.getAsBoolean("strategy.useJti")).thenReturn(false);
        Mockito.when(configContext.getAsString("tokenLife")).thenReturn("20m");
        Mockito.when(configContext.getAsString("refreshTokenLife")).thenReturn("2d");
    }

    private void jtiConfig() {
        basicConfig();

        Mockito.when(configContext.getAsBoolean("strategy.useJti")).thenReturn(true);
    }

    private void permissionsAndScopesConfig() {
        basicConfig();

        Mockito.when(configContext.getAsBoolean("strategy.includePermissions")).thenReturn(true);
        Mockito.when(configContext.getAsBoolean("strategy.includeScopes")).thenReturn(true);
    }

    @BeforeAll
    void setup() {
        configContext = Mockito.mock(ConfigContext.class);
        jtiProvider = Mockito.mock(JtiProvider.class);

        jwtProvider = new JwtProviderImpl(configContext, jtiProvider);
    }

    @Test
    void generate() {
        basicConfig();

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken(), account.getId(), null, null, null);
        verifyToken(tokens.getRefreshToken(), account.getId(), null, null, null);
    }

    @Test
    void generateWithJti() {
        jtiConfig();

        final String jti = UUID.randomUUID().toString();
        Mockito.when(configContext.getAsBoolean("strategy.useJti")).thenReturn(true);
        Mockito.when(jtiProvider.next()).thenReturn(jti);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken(), account.getId(), jti, null, null);
        verifyToken(tokens.getRefreshToken(), account.getId(), null, null, null);
    }

    @Test
    void generateWithPermissionsAndScopes() {
        permissionsAndScopesConfig();

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        System.out.println(tokens);

        verifyToken(tokens.getToken(), account.getId(), null, account.getPermissions(), account.getScopes());
        verifyToken(tokens.getRefreshToken(), account.getId(), null, null, null);
    }

    @Test
    void validate() {
        basicConfig();

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);
        final Optional<DecodedJWT> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        verifyToken(validatedToken.get(), account.getId(), null, null, null);
    }

    @Test
    void validateWithJti() {
        jtiConfig();

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);
        Mockito.when(jtiProvider.validate(jti)).thenReturn(true);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);
        final Optional<DecodedJWT> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        verifyToken(validatedToken.get(), account.getId(), jti, null, null);
    }

    @Test
    void validateWithJtiBlacklisted() {
        jtiConfig();

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);
        Mockito.when(jtiProvider.validate(jti)).thenReturn(false);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);
        final Optional<DecodedJWT> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isEmpty();
    }

    @Test
    void validateWithAlgNone() {
        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);
        final String payload = tokens.getToken().split("\\.")[1];
        final String maliciousToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + payload + ".signature";

        assertThat(jwtProvider.validateToken(maliciousToken)).isEmpty();
    }

    @Test
    void validateWithPermissionsAndScopes() {
        permissionsAndScopesConfig();

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);
        final Optional<DecodedJWT> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        verifyToken(validatedToken.get(), account.getId(), null, account.getPermissions(), account.getScopes());
    }

    private TokensBO getToken(final AccountBO account) {
        return jwtProvider.generateToken(account);
    }

    private void verifyToken(final String token, final String subject, final String jti, final List<PermissionBO> permissions,
                             final List<String> scopes) {
        final Verification verifier = JWT.require(Algorithm.HMAC256(KEY))
                .withIssuer(ISSUER)
                .withSubject(subject);

        if (jti != null) {
            verifier.withJWTId(jti);
        }

        final DecodedJWT decodedJWT = verifier.build().verify(token);

        if (permissions != null) {
            assertThat(decodedJWT.getClaim("permissions").asArray(String.class)).hasSameSizeAs(permissions);
        }

        if (scopes != null) {
            assertThat(decodedJWT.getClaim("scopes").asArray(String.class)).containsExactlyInAnyOrder(scopes.toArray(new String[0]));
        }
    }

    private void verifyToken(final DecodedJWT decodedJWT, final String subject, final String jti, final List<PermissionBO> permissions,
                             final List<String> scopes) {
        final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(KEY))
                .build();

        verifier.verify(decodedJWT);

        assertThat(decodedJWT.getIssuer()).isEqualTo(ISSUER);
        assertThat(decodedJWT.getSubject()).isEqualTo(subject);

        if (jti != null) {
            assertThat(decodedJWT.getId()).isEqualTo(jti);
        }

        if (permissions != null) {
            assertThat(decodedJWT.getClaim("permissions").asArray(String.class)).hasSameSizeAs(permissions);
        }

        if (scopes != null) {
            assertThat(decodedJWT.getClaim("scopes").asArray(String.class)).containsExactlyInAnyOrder(scopes.toArray(new String[0]));
        }
    }
}
