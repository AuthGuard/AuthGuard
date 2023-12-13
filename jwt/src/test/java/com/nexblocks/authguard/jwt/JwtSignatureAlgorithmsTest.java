package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.model.AccountBO;
import io.vavr.control.Either;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Security;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtSignatureAlgorithmsTest {
    private static final String ISSUER = "test";

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private JwtConfig jwtConfig(final String algorithm, final String publicKey, final String privateKey) {
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

    private JwtTokenVerifier createVerifier(final String algorithm, final String publicKey, final String privateKey) {
        final JwtConfig jwtConfig = jwtConfig(algorithm, publicKey, privateKey);
        final Algorithm parsedAlgorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());

        return new JwtTokenVerifier(strategyConfig(), null, parsedAlgorithm);
    }

    private String generateToken(final JwtConfig config) {
        final Algorithm algorithm = JwtConfigParser.parseAlgorithm(config.getAlgorithm(), config.getPublicKey(),
                config.getPrivateKey());
        final JwtGenerator jwtGenerator = new JwtGenerator(config);

        final AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        final JWTCreator.Builder tokenBuilder = jwtGenerator.generateUnsignedToken(account, Duration.ofMinutes(5));

        return tokenBuilder.sign(algorithm);
    }

    @Test
    void generateAndVerifyEc256() {
        final String algorithm = "EC256";
        final String privateKey = "file:src/test/resources/ec256-private.pem";
        final String publicKey = "file:src/test/resources/ec256-public.pem";

        final String token = generateToken(jwtConfig(algorithm, publicKey, privateKey));
        final JwtTokenVerifier tokenVerifier = createVerifier(algorithm, publicKey, privateKey);

        final Either<Exception, DecodedJWT> validatedToken = tokenVerifier.verify(token);

        assertThat(validatedToken.isRight()).isTrue();
        assertThat(validatedToken.get().getAlgorithm()).isEqualTo("ES256");
    }

    @Test
    void generateAndVerifyEc512() {
        final String algorithm = "EC512";
        final String privateKey = "file:src/test/resources/ec512-private.pem";
        final String publicKey = "file:src/test/resources/ec512-public.pem";

        final String token = generateToken(jwtConfig(algorithm, publicKey, privateKey));
        final JwtTokenVerifier tokenVerifier = createVerifier(algorithm, publicKey, privateKey);

        final Either<Exception, DecodedJWT> validatedToken = tokenVerifier.verify(token);

        assertThat(validatedToken.isRight()).isTrue();
        assertThat(validatedToken.get().getAlgorithm()).isEqualTo("ES512");
    }

    @Test
    void generateAndVerifyRsa256() {
        final String algorithm = "RSA256";
        final String privateKey = "file:src/test/resources/rsa256-private.pem";
        final String publicKey = "file:src/test/resources/rsa256-public.pem";

        final String token = generateToken(jwtConfig(algorithm, publicKey, privateKey));
        final JwtTokenVerifier tokenVerifier = createVerifier(algorithm, publicKey, privateKey);

        final Either<Exception, DecodedJWT> validatedToken = tokenVerifier.verify(token);

        assertThat(validatedToken.isRight()).isTrue();
        assertThat(validatedToken.get().getAlgorithm()).isEqualTo("RS256");
    }

    @Test
    void generateAndVerifyRsa512() {
        final String algorithm = "RSA512";
        final String privateKey = "file:src/test/resources/rsa512-private.pem";
        final String publicKey = "file:src/test/resources/rsa512-public.pem";

        final String token = generateToken(jwtConfig(algorithm, publicKey, privateKey));
        final JwtTokenVerifier tokenVerifier = createVerifier(algorithm, publicKey, privateKey);

        final Either<Exception, DecodedJWT> validatedToken = tokenVerifier.verify(token);

        assertThat(validatedToken.isRight()).isTrue();
        assertThat(validatedToken.get().getAlgorithm()).isEqualTo("RS512");
    }
}
