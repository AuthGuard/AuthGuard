package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;

class JwtConfigParserTest {

    @Test
    void parseHmac512() {
        final String privateKeyPath = "src/test/resources/hmac256.pem";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("HMAC256", null, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }

    @Test
    void parseRsa512() {
        final String publicKeyPath = "src/test/resources/rsa512-public.pem";
        final String privateKeyPath = "src/test/resources/rsa512-private.pem";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("RSA512", publicKeyPath, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }

    @Test
    void parseEc256() {
        final String publicKeyPath = "src/test/resources/ec256-public.pem";
        final String privateKeyPath = "src/test/resources/ec256-private.pem";

        final Algorithm algorithm = JwtConfigParser.parseAlgorithm("EC256", publicKeyPath, privateKeyPath);

        final String jwt = JWT.create().withClaim("claim", "value").sign(algorithm);

        algorithm.verify(JWT.decode(jwt));
    }
}