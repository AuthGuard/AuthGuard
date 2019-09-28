package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;

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

        JWTCreator.Builder tokenBuilder = JWT.create()
                .withIssuer(jwtConfig.issuer())
                .withSubject(account.getId())
                // TODO properly handle timezones
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));

        if (jwtConfig.includePermissions()) {
            tokenBuilder.withArrayClaim("permissions", account.getPermissions().stream()
                    .map(this::permissionToString).toArray(String[]::new));
        }

        if (jwtConfig.includeScopes()) {
            tokenBuilder.withArrayClaim("scopes", account.getScopes().toArray(new String[0]));
        }

        return tokenBuilder;
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

    String permissionToString(final PermissionBO permission) {
        return permission.getGroup() + "." + permission.getName();
    }
}
