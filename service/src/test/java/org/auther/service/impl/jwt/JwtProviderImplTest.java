package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auther.config.ConfigContext;
import org.auther.service.JtiProvider;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

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

    private final static EasyRandom RANDOM = new EasyRandom();

    private void nonJtiConfig() {
        Mockito.when(configContext.getAsString("algorithm")).thenReturn(ALGORITHM);
        Mockito.when(configContext.getAsString("key")).thenReturn(KEY);
        Mockito.when(configContext.getAsString("issuer")).thenReturn(ISSUER);
        Mockito.when(configContext.getAsBoolean("strategy.useJti")).thenReturn(false);
        Mockito.when(configContext.getAsString("tokenLife")).thenReturn("20m");
        Mockito.when(configContext.getAsString("refreshTokenLife")).thenReturn("2d");
    }

    private void jtiConfig() {
        Mockito.when(configContext.getAsString("algorithm")).thenReturn(ALGORITHM);
        Mockito.when(configContext.getAsString("key")).thenReturn(KEY);
        Mockito.when(configContext.getAsString("issuer")).thenReturn(ISSUER);
        Mockito.when(configContext.getAsBoolean("strategy.useJti")).thenReturn(true);
        Mockito.when(configContext.getAsString("tokenLife")).thenReturn("20m");
        Mockito.when(configContext.getAsString("refreshTokenLife")).thenReturn("2d");
    }

    @BeforeAll
    void setup() {
        configContext = Mockito.mock(ConfigContext.class);
        jtiProvider = Mockito.mock(JtiProvider.class);

        jwtProvider = new JwtProviderImpl(configContext, jtiProvider);
    }

    @Test
    void generate() {
        nonJtiConfig();

        final TokensBO tokens = getToken();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken());
        verifyToken(tokens.getRefreshToken());
    }

    @Test
    void generateWithJti() {
        jtiConfig();

        final String jti = UUID.randomUUID().toString();
        Mockito.when(configContext.getAsBoolean("strategy.useJti")).thenReturn(true);
        Mockito.when(jtiProvider.next()).thenReturn(jti);

        final TokensBO tokens = getToken();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken(), jti);
        verifyToken(tokens.getRefreshToken());
    }

    @Test
    void validate() {
        nonJtiConfig();

        final TokensBO tokens = getToken();
        final Optional<String> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        assertThat(validatedToken.get()).isEqualTo(tokens.getToken());
    }

    @Test
    void validateWithJti() {
        jtiConfig();

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);
        Mockito.when(jtiProvider.validate(jti)).thenReturn(true);

        final TokensBO tokens = getToken();
        final Optional<String> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        assertThat(validatedToken.get()).isEqualTo(tokens.getToken());
    }

    @Test
    void validateWithJtiBlacklisted() {
        jtiConfig();

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);
        Mockito.when(jtiProvider.validate(jti)).thenReturn(false);

        final TokensBO tokens = getToken();
        final Optional<String> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isEmpty();
    }

    @Test
    void validateWithAlgNone() {
        final TokensBO tokens = getToken();
        final String payload = tokens.getToken().split("\\.")[1];
        final String maliciousToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + payload + ".signature";

        assertThat(jwtProvider.validateToken(maliciousToken)).isEmpty();
    }

    private TokensBO getToken() {
        return jwtProvider.generateToken(RANDOM.nextObject(AccountBO.class));
    }

    private void verifyToken(final String token) {
        final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(KEY))
                .withIssuer(ISSUER)
                .build();

        verifier.verify(token);
    }

    private void verifyToken(final String token, final String jti) {
        final DecodedJWT decodedJWT = JWT.decode(token);

        final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(KEY))
                .build();

        verifier.verify(decodedJWT);

        assertThat(decodedJWT.getIssuer()).isEqualTo(ISSUER);
        assertThat(decodedJWT.getId()).isEqualTo(jti);
    }
}
