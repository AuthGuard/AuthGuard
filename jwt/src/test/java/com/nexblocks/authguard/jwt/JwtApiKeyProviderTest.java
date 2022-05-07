package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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


    private JwtApiKeyProvider newProviderInstance(final JtiProvider jtiProvider) {
        return new JwtApiKeyProvider(jwtConfig(), jtiProvider);
    }
    @Test
    void generateTokenAccount() {
        final JwtApiKeyProvider tokenProvider = newProviderInstance(Mockito.mock(JtiProvider.class));
        assertThatThrownBy(() -> tokenProvider.generateToken(RANDOM.nextObject(AccountBO.class)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void generateTokenApp() {
        final JtiProvider jtiProvider = Mockito.mock(JtiProvider.class);
        final JwtApiKeyProvider tokenProvider = newProviderInstance(jtiProvider);

        final String jti = "tokenId";
        final AppBO app = RANDOM.nextObject(AppBO.class);

        Mockito.when(jtiProvider.next()).thenReturn(jti);

        final AuthResponseBO tokens = tokenProvider.generateToken(app);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        verifyToken(tokens.getToken().toString(), app.getId(), jti);
    }

    private void verifyToken(final String token, final String subject, final String jti) {
        final JWTVerifier verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .withSubject(subject)
                .withJWTId(jti)
                .build();

        final DecodedJWT decodedJWT = verifier.verify(token);

        assertThat(decodedJWT.getClaim("type").asString()).isEqualTo("API");
    }

    private void verifyToken(final DecodedJWT decodedJWT, final String subject, final String jti) {
        final JWTVerifier verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .build();

        verifier.verify(decodedJWT);

        assertThat(decodedJWT.getSubject()).isEqualTo(subject);
        assertThat(decodedJWT.getId()).isEqualTo(jti);
    }
}