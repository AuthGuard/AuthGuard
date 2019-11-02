package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import org.auther.service.impl.jwt.config.ImmutableJwtConfig;
import org.auther.service.model.AccountBO;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

class TokenGenerator {
    private static final int RANDOM_SIZE = 1024;

    private final ImmutableJwtConfig jwtConfig;

    private final SecureRandom secureRandom;

    TokenGenerator(final ImmutableJwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;

        secureRandom = new SecureRandom();
    }

    // TODO add token type
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

    // TODO add token type
    JWTCreator.Builder generateRandomRefreshToken(final AccountBO account, final Duration tokenLife) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime exp = now.plus(tokenLife);

        return JWT.create()
                .withIssuer(jwtConfig.getIssuer())
                .withSubject(account.getId())
                // TODO properly handle timezones
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));
    }

    String generateRandomRefreshToken() {
        final byte[] bytes = new byte[RANDOM_SIZE];

        secureRandom.nextBytes(bytes);

        return new String(bytes);
    }
}
