package org.auther.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BasicJWTHandlerTest {
    private BasicJWTHandler jwtProvider;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        final Algorithm algorithm = Algorithm.HMAC256("this secret is only for testing purposes");
        final JWTVerifier verifier = JWT.require(algorithm).build();
        jwtProvider = new BasicJWTHandler(algorithm, verifier);
    }

    @Test
    @Order(1)
    void generate() {
        final TokensBO tokens = getToken();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());
    }

    @Test
    @Order(2)
    void validate() {
        final TokensBO tokens = getToken();
        final Optional<String> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        assertThat(validatedToken.get()).isEqualTo(tokens.getToken()); // a basic verifier should not create a new token
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
}
