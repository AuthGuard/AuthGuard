package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.util.AsyncUtils;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Try;

public class JwtTokenVerifier implements AuthVerifier {
    private final StrategyConfig strategy;
    private final JtiProvider jti;
    private final JWTVerifier verifier;

    public JwtTokenVerifier(final StrategyConfig strategy, final JtiProvider jti,
                            final Algorithm algorithm) {
        this.strategy = strategy;
        this.jti = jti;

        this.verifier = JWT.require(algorithm).build();
    }

    public JwtTokenVerifier(final StrategyConfig strategy, final Algorithm algorithm) {
        this.strategy = strategy;
        this.jti = null;

        this.verifier = JWT.require(algorithm).build();
    }

    Try<DecodedJWT> verify(final String token) {
        try {
            DecodedJWT decoded = JWT.decode(token);
            DecodedJWT verified = verifier.verify(decoded);

            if (this.verifyJti(verified)) {
                return Try.success(verified);
            } else {
                return Try.failure(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid JTI"));
            }
        } catch (final JWTVerificationException e) {
            return Try.failure(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE, "Invalid JWT"));
        }
    }

    private boolean verifyJti(final DecodedJWT decoded) {
        return !strategy.useJti() || jti.validate(decoded.getId());
    }

    @Override
    public Uni<Long> verifyAccountToken(String token) {
        return AsyncUtils.uniFromTry(verify(token)
                .flatMap(payload -> {
                    try {
                        return Try.success(Long.parseLong(payload.getSubject()));
                    } catch (Exception e) {
                        return Try.failure(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE, "Invalid JWT subject"));
                    }
                }));
    }
}
