package com.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.authguard.config.ConfigContext;
import com.authguard.config.JacksonConfigContext;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.PermissionBO;
import com.authguard.service.model.TokensBO;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdTokenProviderTest {
    private static final String ALGORITHM = "HMAC256";
    private static final String KEY = "this secret is only for testing purposes";
    private static final String ISSUER = "test";

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private ConfigContext jwtConfig() {
        final ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);

        configNode.put("algorithm", ALGORITHM)
                .put("privateKey", KEY)
                .put("issuer", ISSUER);

        return new JacksonConfigContext(configNode);
    }

    private ConfigContext strategyConfig() {
        final ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance);

        configNode.put("tokenLife", "5m")
                .put("refreshTokenLife", "20m")
                .put("includePermissions", true);

        return new JacksonConfigContext(configNode);
    }

    private IdTokenProvider newProviderInstance() {
        return new IdTokenProvider(jwtConfig(), strategyConfig());
    }

    @Test
    void generate() {
        final IdTokenProvider idTokenProvider = newProviderInstance();

        final AccountBO account = RANDOM.nextObject(AccountBO.class);
        final TokensBO tokens = idTokenProvider.generateToken(account);

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());

        verifyToken(tokens.getToken().toString(), account.getId(), null, null, null);
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
