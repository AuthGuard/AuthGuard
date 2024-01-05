package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.model.AccountBO;
import io.vavr.control.Try;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Security;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtSignatureAlgorithmsTest {
    private static String ISSUER = "test";

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private JwtConfig jwtConfig(String algorithm, String publicKey, String privateKey) {
        return JwtConfig.builder()
                .algorithm(algorithm)
                .publicKey(publicKey)
                .privateKey(privateKey)
                .issuer(ISSUER)
                .build();
    }

    private StrategyConfig strategyConfig() {
        return StrategyConfig.builder()
                .tokenLife("5m")
                .includePermissions(true)
                .build();
    }

    private JwtTokenVerifier createVerifier(String algorithm, String publicKey, String privateKey) {
        JwtConfig jwtConfig = jwtConfig(algorithm, publicKey, privateKey);
        Algorithm parsedAlgorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());

        return new JwtTokenVerifier(strategyConfig(), null, parsedAlgorithm);
    }

    private String generateToken(JwtConfig config) {
        Algorithm algorithm = JwtConfigParser.parseAlgorithm(config.getAlgorithm(), config.getPublicKey(),
                config.getPrivateKey());
        JwtGenerator jwtGenerator = new JwtGenerator(config);

        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        JWTCreator.Builder tokenBuilder = jwtGenerator.generateUnsignedToken(account, Duration.ofMinutes(5));

        return tokenBuilder.sign(algorithm);
    }

    @Test
    void generateAndVerifyEc256() {
        String algorithm = "EC256";
        String privateKey = "file:src/test/resources/ec256-private.pem";
        String publicKey = "file:src/test/resources/ec256-public.pem";

        String token = generateToken(jwtConfig(algorithm, publicKey, privateKey));
        JwtTokenVerifier tokenVerifier = createVerifier(algorithm, publicKey, privateKey);

        Try<DecodedJWT> validatedToken = tokenVerifier.verify(token);

        assertThat(validatedToken.isSuccess()).isTrue();
        assertThat(validatedToken.get().getAlgorithm()).isEqualTo("ES256");
    }

    @Test
    void generateAndVerifyEc512() {
        String algorithm = "EC512";
        String privateKey = "file:src/test/resources/ec512-private.pem";
        String publicKey = "file:src/test/resources/ec512-public.pem";

        String token = generateToken(jwtConfig(algorithm, publicKey, privateKey));
        JwtTokenVerifier tokenVerifier = createVerifier(algorithm, publicKey, privateKey);

        Try<DecodedJWT> validatedToken = tokenVerifier.verify(token);

        assertThat(validatedToken.isSuccess()).isTrue();
        assertThat(validatedToken.get().getAlgorithm()).isEqualTo("ES512");
    }

    @Test
    void generateAndVerifyRsa256() {
        String algorithm = "RSA256";
        String privateKey = "file:src/test/resources/rsa256-private.pem";
        String publicKey = "file:src/test/resources/rsa256-public.pem";

        String token = generateToken(jwtConfig(algorithm, publicKey, privateKey));
        JwtTokenVerifier tokenVerifier = createVerifier(algorithm, publicKey, privateKey);

        Try<DecodedJWT> validatedToken = tokenVerifier.verify(token);

        assertThat(validatedToken.isSuccess()).isTrue();
        assertThat(validatedToken.get().getAlgorithm()).isEqualTo("RS256");
    }

    @Test
    void generateAndVerifyRsa512() {
        String algorithm = "RSA512";
        String privateKey = "file:src/test/resources/rsa512-private.pem";
        String publicKey = "file:src/test/resources/rsa512-public.pem";

        String token = generateToken(jwtConfig(algorithm, publicKey, privateKey));
        JwtTokenVerifier tokenVerifier = createVerifier(algorithm, publicKey, privateKey);

        Try<DecodedJWT> validatedToken = tokenVerifier.verify(token);

        assertThat(validatedToken.isSuccess()).isTrue();
        assertThat(validatedToken.get().getAlgorithm()).isEqualTo("RS512");
    }
}
