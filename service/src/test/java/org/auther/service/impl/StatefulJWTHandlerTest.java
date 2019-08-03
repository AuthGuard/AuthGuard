package org.auther.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.auther.service.JTIProvider;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatefulJWTHandlerTest {
    private JTIProvider jtiProvider;
    private StatefulJWTHandler jwtProvider;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        final Algorithm algorithm = Algorithm.HMAC256("this secret is only for testing purposes");
        final JWTVerifier verifier = JWT.require(algorithm).build();

        jtiProvider = Mockito.mock(JTIProvider.class);
        jwtProvider = new StatefulJWTHandler(algorithm, verifier, jtiProvider);
    }

    @Test
    @Order(1)
    void generate() {
        final TokensBO tokens = getToken();

        Mockito.when(jtiProvider.next()).thenReturn(UUID.randomUUID().toString());

        assertThat(tokens).isNotNull();
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNotNull();
        assertThat(tokens.getToken()).isNotEqualTo(tokens.getRefreshToken());
    }

    @Test
    @Order(2)
    void validate() {
        final TokensBO tokens = getToken();

        Mockito.when(jtiProvider.next()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(jtiProvider.validate(any())).thenReturn(true);

        final Optional<String> validatedToken = jwtProvider.validateToken(tokens.getToken());

        assertThat(validatedToken).isNotEmpty();
        assertThat(validatedToken.get()).isNotEqualTo(tokens.getToken());
    }

    @Test
    void validateInvalidJTI() {
        final TokensBO tokens = getToken();

        Mockito.when(jtiProvider.next()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(jtiProvider.validate(any())).thenReturn(false);

        assertThat(jwtProvider.validateToken(tokens.getToken())).isEmpty();
    }

    @Test
    void validateWithAlgNone() {
        final TokensBO tokens = getToken();
        final String payload = tokens.getToken().split("\\.")[1];
        final String maliciousToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + payload + ".signature";

        Mockito.when(jtiProvider.next()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(jtiProvider.validate(any())).thenReturn(true);

        assertThat(jwtProvider.validateToken(maliciousToken)).isEmpty();
    }

    private TokensBO getToken() {
        return jwtProvider.generateToken(RANDOM.nextObject(AccountBO.class));
    }
}