package com.authguard.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.authguard.service.config.JwtConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiTokenProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "this secret is only for testing purposes";

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private JwtConfig jwtConfig() {
        return JwtConfig.builder()
                .algorithm(ALGORITHM)
                .key(KEY)
                .build();
    }


    private ApiTokenProvider newProviderInstance(final JtiProvider jtiProvider) {
        return new ApiTokenProvider(jwtConfig(), jtiProvider);
    }
    @Test
    void generateTokenAccount() {
        final ApiTokenProvider tokenProvider = newProviderInstance(Mockito.mock(JtiProvider.class));
        assertThatThrownBy(() -> tokenProvider.generateToken(RANDOM.nextObject(AccountBO.class)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void generateTokenApp() {
        final JtiProvider jtiProvider = Mockito.mock(JtiProvider.class);
        final ApiTokenProvider tokenProvider = newProviderInstance(jtiProvider);

        final String jti = "tokenId";
        final AppBO app = RANDOM.nextObject(AppBO.class);

        Mockito.when(jtiProvider.next()).thenReturn(jti);

        final TokensBO tokens = tokenProvider.generateToken(app);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        verifyToken(tokens.getToken(), app.getId(), jti);
    }

    private void verifyToken(final String token, final String subject, final String jti) {
        final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(KEY))
                .withSubject(subject)
                .withJWTId(jti)
                .build();

        final DecodedJWT decodedJWT = verifier.verify(token);

        assertThat(decodedJWT.getClaim("type").asString()).isEqualTo("API");
    }

    private void verifyToken(final DecodedJWT decodedJWT, final String subject, final String jti) {
        final JWTVerifier verifier = JWT.require(Algorithm.HMAC256(KEY))
                .build();

        verifier.verify(decodedJWT);

        assertThat(decodedJWT.getSubject()).isEqualTo(subject);
        assertThat(decodedJWT.getId()).isEqualTo(jti);
    }
}