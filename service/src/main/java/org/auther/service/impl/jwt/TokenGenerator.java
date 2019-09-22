package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import org.auther.service.model.AccountBO;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

class TokenGenerator {
    private final JwtConfig jwtConfig;

    TokenGenerator(final JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    JWTCreator.Builder generateUnsignedToken(final AccountBO account) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime exp = now.plus(jwtConfig.tokenLife());

        ZoneId.systemDefault().getRules().getOffset(now);

        return JWT.create()
                .withIssuer(jwtConfig.issuer())
                .withSubject(account.getId())
                // TODO properly handle timezones
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));
    }

    JWTCreator.Builder generateUnsignedRefreshToken(final AccountBO account) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime exp = now.plus(jwtConfig.refreshTokenLife());

        return JWT.create()
                .withIssuer(jwtConfig.issuer())
                .withSubject(account.getId())
                // TODO properly handle timezones
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));
    }
}
