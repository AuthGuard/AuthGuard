package com.authguard.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.authguard.service.config.JwtConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.random.CryptographicRandom;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class JwtGenerator {
    private static final int RANDOM_SIZE = 128;

    private final JwtConfig jwtConfig;

    private final CryptographicRandom random;

    JwtGenerator(final JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;

        this.random = new CryptographicRandom();
    }

    JWTCreator.Builder generateUnsignedToken(final AccountBO account, final Duration tokenLife) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime exp = now.plus(tokenLife);

        ZoneId.systemDefault().getRules().getOffset(now);

        return JWT.create()
                .withIssuer(jwtConfig.getIssuer())
                .withSubject(account.getId())
                // TODO properly handle timezones
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));
    }

    String generateRandomRefreshToken() {
        return random.base64(RANDOM_SIZE);
    }
}
