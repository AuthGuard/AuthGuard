package com.authguard.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.authguard.service.config.ImmutableJwtConfig;
import com.authguard.service.config.ImmutableStrategiesConfig;
import com.authguard.service.config.ImmutableStrategyConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdTokenProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "this secret is only for testing purposes";
    private static final String ISSUER = "test";

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private ImmutableJwtConfig jwtConfig(final ImmutableStrategyConfig strategyConfig) {
        return ImmutableJwtConfig.builder()
                .algorithm(ALGORITHM)
                .key(KEY)
                .issuer(ISSUER)
                .strategies(ImmutableStrategiesConfig.builder()
                        .idToken(strategyConfig)
                        .build())
                .build();
    }

    private ImmutableStrategyConfig strategyConfig() {
        return ImmutableStrategyConfig.builder()
                .tokenLife("5m")
                .includePermissions(true)
                .build();
    }

    private IdTokenProvider newProviderInstance(final ImmutableStrategyConfig strategyConfig) {
        return new IdTokenProvider(jwtConfig(strategyConfig));
    }

    @Test
    void generate() {
        final ImmutableStrategyConfig strategyConfig = strategyConfig();

        final IdTokenProvider idTokenProvider = newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = idTokenProvider.generateToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken(), account.getId(), null, null, null);
    }

    @Test
    void validate() {
        final ImmutableStrategyConfig strategyConfig = strategyConfig();

        final IdTokenProvider idTokenProvider = newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = idTokenProvider.generateToken(account);
        final Optional<DecodedJWT> validatedToken = idTokenProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        verifyToken(validatedToken.get(), account.getId());
    }

    @Test
    void validateWithAlgNone() {
        final ImmutableStrategyConfig strategyConfig = strategyConfig();

        final IdTokenProvider idTokenProvider = newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = idTokenProvider.generateToken(account);
        final String payload = tokens.getToken().split("\\.")[1];
        final String maliciousToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + payload + ".signature";

        assertThat(idTokenProvider.validateToken(maliciousToken)).isEmpty();
    }

    private void verifyToken(final String token, final String subject, final String jti,
                             final List<PermissionBO> permissions, final List<String> scopes) {
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

    private void verifyToken(final DecodedJWT decodedJWT, final String subject) {
        final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(KEY))
                .build();

        verifier.verify(decodedJWT);

        assertThat(decodedJWT.getIssuer()).isEqualTo(ISSUER);
        assertThat(decodedJWT.getSubject()).isEqualTo(subject);
    }
}
