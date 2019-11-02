package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import org.auther.service.JtiProvider;
import org.auther.service.JwtScenario;
import org.auther.service.JwtStrategy;
import org.auther.service.impl.jwt.config.*;
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

    private JtiProvider jtiProvider;
    private JwtProviderImpl jwtProvider;
    private JwtStrategy jwtStrategy;

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    // TODO clean up this config mess and use a resource file
    private ModifiableJwtConfig jwtConfig() {
        final ModifiableJwtConfig config = new ModifiableJwtConfig();

        config.setAlgorithm(ALGORITHM);
        config.setKey(KEY);
        config.setIssuer(ISSUER);

        return config;
    }

    private JwtProviderImpl newProviderInstance(final ImmutableStrategyConfig strategyConfig) {
        jtiProvider = Mockito.mock(JtiProvider.class);
        jwtStrategy = Mockito.mock(JwtStrategy.class);

        Mockito.when(jwtStrategy.getConfig()).thenReturn(strategyConfig);

        jwtProvider = new JwtProviderImpl(jwtConfig().toImmutable(), jwtStrategy, jtiProvider);

        return jwtProvider;
    }

    @Test
    void generate() {
        final ImmutableStrategyConfig strategyConfig = ImmutableStrategyConfig.builder()
                .useJti(false)
                .signedRefreshTokens(false)
                .build();

        newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken(), account.getId(), null, null, null);
    }

    @Test
    void generateWithJti() {
        final ImmutableStrategyConfig strategyConfig = ImmutableStrategyConfig.builder()
                .useJti(true)
                .signedRefreshTokens(false)
                .build();

        newProviderInstance(strategyConfig);

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account, jti);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken(), account.getId(), jti, null, null);
    }

    @Test
    void validate() {
        final ImmutableStrategyConfig strategyConfig = ImmutableStrategyConfig.builder()
                .useJti(false)
                .signedRefreshTokens(false)
                .build();

        newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);
        final Optional<DecodedJWT> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        verifyToken(validatedToken.get(), account.getId(), null, null, null);
    }

    @Test
    void validateWithJti() {
        final ImmutableStrategyConfig strategyConfig = ImmutableStrategyConfig.builder()
                .useJti(true)
                .signedRefreshTokens(false)
                .build();

        newProviderInstance(strategyConfig);

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);
        Mockito.when(jtiProvider.validate(jti)).thenReturn(true);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account, jti);
        final Optional<DecodedJWT> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        verifyToken(validatedToken.get(), account.getId(), jti, null, null);
    }

    @Test
    void validateWithJtiBlacklisted() {
        final ImmutableStrategyConfig strategyConfig = ImmutableStrategyConfig.builder()
                .useJti(true)
                .signedRefreshTokens(false)
                .build();

        newProviderInstance(strategyConfig);

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);
        Mockito.when(jtiProvider.validate(jti)).thenReturn(false);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account, jti);
        final Optional<DecodedJWT> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isEmpty();
    }

    @Test
    void validateWithAlgNone() {
        final ImmutableStrategyConfig strategyConfig = ImmutableStrategyConfig.builder()
                .useJti(false)
                .signedRefreshTokens(false)
                .build();

        newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = getToken(account);
        final String payload = tokens.getToken().split("\\.")[1];
        final String maliciousToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + payload + ".signature";

        assertThat(jwtProvider.validateToken(maliciousToken)).isEmpty();
    }

    private TokensBO getToken(final AccountBO account) {
        final JWTCreator.Builder token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(account.getId());

        Mockito.when(jwtStrategy.generateToken(account)).thenReturn(token);
        Mockito.when(jwtStrategy.generateRefreshToken(account)).thenReturn(RANDOM.nextObject(String.class));

        return jwtProvider.generateToken(account, JwtScenario.USER_TOKEN);
    }

    private TokensBO getToken(final AccountBO account, final String jti) {
        final JWTCreator.Builder token = JWT.create()
                .withJWTId(jti)
                .withIssuer(ISSUER)
                .withSubject(account.getId());

        Mockito.when(jwtStrategy.generateToken(account)).thenReturn(token);
        Mockito.when(jwtStrategy.generateRefreshToken(account)).thenReturn(RANDOM.nextObject(String.class));

        return jwtProvider.generateToken(account, JwtScenario.USER_TOKEN);
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
