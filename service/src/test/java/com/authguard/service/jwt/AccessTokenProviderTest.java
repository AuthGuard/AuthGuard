package com.authguard.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.authguard.service.config.*;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.TokenBuilderBO;
import com.authguard.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessTokenProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "this secret is only for testing purposes";
    private static final String ISSUER = "test";

    private JtiProvider jtiProvider;

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private ImmutableJwtConfig jwtConfig(final ImmutableStrategyConfig strategyConfig) {
        return ImmutableJwtConfig.builder()
                .algorithm(ALGORITHM)
                .key(KEY)
                .issuer(ISSUER)
                .strategies(ImmutableStrategiesConfig.builder()
                        .accessToken(strategyConfig)
                        .build())
                .build();
    }

    private ImmutableStrategyConfig strategyConfig(final boolean useJti) {
        return ImmutableStrategyConfig.builder()
                .tokenLife("5m")
                .useJti(useJti)
                .includePermissions(true)
                .build();
    }

    private AccessTokenProvider newProviderInstance(final ImmutableStrategyConfig strategyConfig) {
        jtiProvider = Mockito.mock(JtiProvider.class);

        return new AccessTokenProvider(jwtConfig(strategyConfig), jtiProvider);
    }

    @Test
    void generate() {
        final ImmutableStrategyConfig strategyConfig = strategyConfig(false);

        final AccessTokenProvider accessTokenProvider = newProviderInstance(strategyConfig);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = accessTokenProvider.generateToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken(), account.getId(), null, null, null);
    }

    @Test
    void generateWithJti() {
        final ImmutableStrategyConfig strategyConfig = strategyConfig(true);

        final AccessTokenProvider accessTokenProvider = newProviderInstance(strategyConfig);

        final String jti = UUID.randomUUID().toString();

        Mockito.when(jtiProvider.next()).thenReturn(jti);

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = accessTokenProvider.generateToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken(), account.getId(), jti, null, null);
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
}
