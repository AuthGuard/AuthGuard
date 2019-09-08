package org.auther.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.auther.service.model.AccountBO;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

abstract class AbstractJWTHandler {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    AbstractJWTHandler(Algorithm algorithm, JWTVerifier verifier) {
        this.algorithm = algorithm;
        this.verifier = verifier;
    }

    JWTCreator.Builder generateUnsignedToken(final AccountBO account, final LocalDateTime now) {
        final LocalDateTime exp = now.plusMinutes(20);

        ZoneId.systemDefault().getRules().getOffset(now);

        return JWT.create()
                .withIssuer(this.getClass().getName())
                // TODO properly handle timezones
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));
    }

    JWTCreator.Builder generateUnsignedRefreshToken(final AccountBO account, final LocalDateTime now) {
        final LocalDateTime exp = now.plusDays(1);

        return JWT.create()
                .withIssuer(this.getClass().getName())
                .withIssuedAt(Date.from(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now))))
                .withExpiresAt(Date.from(exp.toInstant(ZoneId.systemDefault().getRules().getOffset(now))));
    }

    Optional<DecodedJWT> decodeAndVerify(final String token) {
        try {
            return Optional.of(JWT.decode(token))
                    .map(verifier::verify);
        } catch (final JWTVerificationException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    Algorithm getAlgorithm() {
        return algorithm;
    }

    JWTCreator.Builder toBuilder(final DecodedJWT decodedJWT) {
        JWTCreator.Builder builder = JWT.create();

        // TODO find a better way
        for (final Map.Entry<String, Claim> claim : decodedJWT.getClaims().entrySet()) {
            builder = builder.withClaim(claim.getKey(), claim.getValue().asString());
        }

        return builder;
    }
}
