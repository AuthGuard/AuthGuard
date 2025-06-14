package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.nexblocks.authguard.jwt.crypto.TokenEncryptorAdapter;
import com.nexblocks.authguard.service.config.EncryptionConfig;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.PermissionBO;
import io.vavr.control.Either;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdTokenProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "file:src/test/resources/hmac256.pem";
    private static final String ISSUER = "test";

    private static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters()
            .excludeField(field -> field.getName().equals("initShim"))
            .collectionSizeRange(1, 4));
    private TokenEncryptorAdapter tokenEncryptor;

    private JwtConfig jwtConfig() {
        return JwtConfig.builder()
                .algorithm(ALGORITHM)
                .privateKey(KEY)
                .issuer(ISSUER)
                .build();
    }

    private JwtConfig jwtConfigWithEncryption() {
        return JwtConfig.builder()
                .algorithm(ALGORITHM)
                .privateKey(KEY)
                .issuer(ISSUER)
                .encryption(EncryptionConfig.builder().build())
                .build();
    }

    private StrategyConfig strategyConfig() {
        return StrategyConfig.builder()
                .tokenLife("5m")
                .refreshTokenLife("20m")
                .build();
    }

    private IdTokenProvider newProviderInstance(final JwtConfig jwtConfig) {
        tokenEncryptor = Mockito.mock(TokenEncryptorAdapter.class);

        return new IdTokenProvider(jwtConfig, strategyConfig(), tokenEncryptor);
    }

    @Test
    void generate() {
        IdTokenProvider idTokenProvider = newProviderInstance(jwtConfig());

        AccountBO account = RANDOM.nextObject(AccountBO.class).withActive(true);
        AuthResponseBO tokens = idTokenProvider.generateToken(account).subscribeAsCompletionStage().join();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken().toString(), account.getId(), null, null, null);
    }

    @Test
    void generateEncrypted() {
        IdTokenProvider idTokenProvider = newProviderInstance(jwtConfigWithEncryption());

        Mockito.when(tokenEncryptor.encryptAndEncode(Mockito.any()))
                .thenAnswer(invocation -> Either.right("encrypted"));

        AccountBO account = RANDOM.nextObject(AccountBO.class).withActive(true);
        AuthResponseBO tokens = idTokenProvider.generateToken(account).subscribeAsCompletionStage().join();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isEqualTo("encrypted");
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());
    }

    private void verifyToken(final String token, long subject, String jti,
                             List<PermissionBO> permissions, List<String> scopes) {
        Verification verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .withIssuer(ISSUER)
                .withSubject("" + subject);

        if (jti != null) {
            verifier.withJWTId(jti);
        }

        DecodedJWT decodedJWT = verifier.build().verify(token);

        if (permissions != null) {
            assertThat(decodedJWT.getClaim("permissions").asArray(String.class)).hasSameSizeAs(permissions);
        }

        if (scopes != null) {
            assertThat(decodedJWT.getClaim("scopes").asArray(String.class)).containsExactlyInAnyOrder(scopes.toArray(new String[0]));
        }
    }

    private void verifyToken(final DecodedJWT decodedJWT, String subject) {
        JWTVerifier verifier = JWT.require(JwtConfigParser.parseAlgorithm(ALGORITHM, null, KEY))
                .build();

        verifier.verify(decodedJWT);

        assertThat(decodedJWT.getIssuer()).isEqualTo(ISSUER);
        assertThat(decodedJWT.getSubject()).isEqualTo(subject);
    }
}
