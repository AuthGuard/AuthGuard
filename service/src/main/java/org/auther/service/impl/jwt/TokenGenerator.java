package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import org.auther.service.model.AccountBO;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TokenGenerator {
    private final JWTConfig jwtConfig;

    public TokenGenerator(final JWTConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    JWTCreator.Builder generateUnsignedToken(final AccountBO account) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime exp = now.plusMinutes(20);

        ZoneId.systemDefault().getRules().getOffset(now);

        return JWT.create()
                .withIssuer(jwtConfig.issuer())
                // TODO properly handle timezones
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));
    }

    JWTCreator.Builder generateUnsignedRefreshToken(final AccountBO account) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime exp = now.plusDays(1);

        return JWT.create()
                .withIssuer(jwtConfig.issuer())
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));
    }
}
