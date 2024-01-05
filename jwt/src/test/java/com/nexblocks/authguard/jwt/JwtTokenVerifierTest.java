package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import io.vavr.control.Try;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenVerifierTest {
    private static String ALGORITHM = "HMAC256";
    private static String KEY = "file:src/test/resources/hmac256.pem";
    private static String ISSUER = "test";

    private JtiProvider jtiProvider;

    private static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private JwtConfig jwtConfig() {
        return JwtConfig.builder()
                .algorithm(ALGORITHM)
                .privateKey(KEY)
                .issuer(ISSUER)
                .build();
    }

    private StrategyConfig strategyConfig(boolean useJti) {
        return StrategyConfig.builder()
                .tokenLife("5m")
                .useJti(useJti)
                .includePermissions(true)
                .build();
    }

    private JwtTokenVerifier newVerifierInstance(StrategyConfig strategyConfig) {
        jtiProvider = Mockito.mock(JtiProvider.class);

        JwtConfig jwtConfig = jwtConfig();
        Algorithm algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());

        return new JwtTokenVerifier(strategyConfig, jtiProvider, algorithm);
    }

    private AuthResponseBO generateToken(JwtConfig jwtConfig, AccountBO account, String jti) {
        Algorithm algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());
        JwtGenerator jwtGenerator = new JwtGenerator(jwtConfig);

        JWTCreator.Builder tokenBuilder = jwtGenerator.generateUnsignedToken(account, Duration.ofMinutes(5));

        if (jti != null) {
            tokenBuilder.withJWTId(jti);
        }

        String token = tokenBuilder.sign(algorithm);
        String refreshToken = jwtGenerator.generateRandomRefreshToken();

        return AuthResponseBO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }
    
    @Test
    void validate() {
        StrategyConfig strategyConfig = strategyConfig(false);
        JwtConfig jwtConfig = jwtConfig();

        JwtTokenVerifier jwtTokenVerifier = newVerifierInstance(strategyConfig);

        AccountBO account = RANDOM.nextObject(AccountBO.class);
        AuthResponseBO tokens = generateToken(jwtConfig, account, null);
        Try<DecodedJWT> validatedToken = jwtTokenVerifier.verify(tokens.getToken().toString());

        assertThat(validatedToken.isSuccess()).isTrue();
        verifyToken(validatedToken.get(), account.getId(), null, null, null);
    }

    @Test
    void validateExpired() {
        StrategyConfig strategyConfig = strategyConfig(false);
        JwtConfig jwtConfig = jwtConfig();

        AccountBO account = RANDOM.nextObject(AccountBO.class);

        Algorithm algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());
        JwtGenerator jwtGenerator = new JwtGenerator(jwtConfig);

        String token = jwtGenerator.generateUnsignedToken(account, Duration.ofMinutes(5))
                .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
                .sign(algorithm);

        JwtTokenVerifier jwtTokenVerifier = newVerifierInstance(strategyConfig);

        Try<DecodedJWT> validatedToken = jwtTokenVerifier.verify(token);

        assertThat(validatedToken.isFailure()).isTrue();
        assertThat(validatedToken.getCause()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void validateWithJti() {
        StrategyConfig strategyConfig = strategyConfig(true);
        JwtConfig jwtConfig = jwtConfig();

        JwtTokenVerifier jwtTokenVerifier = newVerifierInstance(strategyConfig);

        String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);
        Mockito.when(jtiProvider.validate(jti)).thenReturn(true);

        AccountBO account = RANDOM.nextObject(AccountBO.class);
        AuthResponseBO tokens = generateToken(jwtConfig, account, jti);
        Try<DecodedJWT> validatedToken = jwtTokenVerifier.verify(tokens.getToken().toString());

        assertThat(validatedToken.isSuccess()).isTrue();
        verifyToken(validatedToken.get(), account.getId(), jti, null, null);
    }

    @Test
    void validateWithJtiBlacklisted() {
        StrategyConfig strategyConfig = strategyConfig(true);
        JwtConfig jwtConfig = jwtConfig();

        JwtTokenVerifier jwtTokenVerifier = newVerifierInstance(strategyConfig);

        String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);
        Mockito.when(jtiProvider.validate(jti)).thenReturn(false);

        AccountBO account = RANDOM.nextObject(AccountBO.class);
        AuthResponseBO tokens = generateToken(jwtConfig, account, jti);
        Try<DecodedJWT> validatedToken = jwtTokenVerifier.verify(tokens.getToken().toString());

        assertThat(validatedToken.isFailure()).isTrue();
    }

    @Test
    void validateWithAlgNone() {
        StrategyConfig strategyConfig = strategyConfig(false);
        JwtConfig jwtConfig = jwtConfig();

        JwtTokenVerifier jwtTokenVerifier = newVerifierInstance(strategyConfig);

        AccountBO account = RANDOM.nextObject(AccountBO.class);
        AuthResponseBO tokens = generateToken(jwtConfig, account, null);
        String payload = tokens.getToken().toString().split("\\.")[1];
        String maliciousToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + payload + ".signature";

        assertThat(jwtTokenVerifier.verify(maliciousToken)).isEmpty();
    }

    private void verifyToken(DecodedJWT decodedJWT, long subject, String jti, List<PermissionBO> permissions,
                             List<String> scopes) {
        JWTVerifier verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .build();

        verifier.verify(decodedJWT);

        assertThat(decodedJWT.getIssuer()).isEqualTo(ISSUER);
        assertThat(decodedJWT.getSubject()).isEqualTo("" + subject);

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