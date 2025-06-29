package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.impl.NullClaim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtApiKeyProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "file:src/test/resources/hmac256.pem";

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private JwtConfig jwtConfig() {
        return JwtConfig.builder()
                .algorithm(ALGORITHM)
                .privateKey(KEY)
                .build();
    }

    private JwtApiKeyProvider newProviderInstance(final boolean includeAccessDetails) {
        final StrategyConfig strategyConfig = StrategyConfig.builder()
                .includeRoles(includeAccessDetails)
                .includePermissions(includeAccessDetails)
                .includeExternalId(includeAccessDetails)
                .build();

        return new JwtApiKeyProvider(jwtConfig(), strategyConfig);
    }

    @Test
    void generateTokenAccount() {
        final JwtApiKeyProvider tokenProvider = newProviderInstance(false);
        assertThatThrownBy(() -> tokenProvider.generateToken(RANDOM.nextObject(AccountBO.class)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void generateTokenApp() {
        final JwtApiKeyProvider tokenProvider = newProviderInstance(false);

        final AppBO app = RANDOM.nextObject(AppBO.class);

        final AuthResponseBO tokens = tokenProvider.generateToken(app);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        verifyToken(tokens.getToken().toString(), app.getId());
    }

    @Test
    void generateTokenAppWithAccessDetails() {
        final JwtApiKeyProvider tokenProvider = newProviderInstance(true);

        final AppBO app = RANDOM.nextObject(AppBO.class)
                .withExternalId("externalId")
                .withRoles("test-role")
                .withPermissions(PermissionBO.builder()
                        .group("test")
                        .name("read")
                        .build());

        final AuthResponseBO tokens = tokenProvider.generateToken(app);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        final DecodedJWT decodedJWT = verifyAndGetDecodedToken(tokens.getToken().toString(), app.getId());

        assertThat(decodedJWT.getClaim("eid").asString())
                .isEqualTo("externalId");

        assertThat(decodedJWT.getClaim("roles").asArray(String.class))
                .containsExactly("test-role");

        assertThat(decodedJWT.getClaim("permissions").asArray(String.class))
                .containsExactly("test:read");
    }

    @Test
    void generateTokenWithExpiry() {
        final JwtApiKeyProvider tokenProvider = newProviderInstance(false);

        final AppBO app = RANDOM.nextObject(AppBO.class);
        final Instant expiresAt = Instant.now().plusSeconds(5);

        final AuthResponseBO tokens = tokenProvider.generateToken(app, expiresAt);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        final DecodedJWT decodedJWT = verifyAndGetDecodedToken(tokens.getToken().toString(), app.getId());

        assertThat(decodedJWT.getExpiresAt()).isNotNull();
        assertThat(decodedJWT.getExpiresAt()).isBetween(
                Instant.now().minusSeconds(1),
                Instant.now().plusSeconds(6));
    }

    private void verifyToken(final String token, final long subject) {
        final JWTVerifier verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .withSubject("" + subject)
                .build();

        final DecodedJWT decodedJWT = verifier.verify(token);

        assertThat(decodedJWT.getClaim("roles")).isInstanceOf(NullClaim.class);
        assertThat(decodedJWT.getClaim("type").asString()).isEqualTo("API");
    }

    private DecodedJWT verifyAndGetDecodedToken(final String token,
                                                final long subject) {
        final JWTVerifier verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .withSubject("" + subject)
                .build();

        final DecodedJWT decodedJWT = verifier.verify(token);

        assertThat(decodedJWT.getClaim("type").asString()).isEqualTo("API");

        return decodedJWT;
    }
}