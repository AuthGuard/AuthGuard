package com.authguard.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Payload;
import com.authguard.service.AuthTokenVerfier;
import com.authguard.service.config.ImmutableStrategyConfig;

import java.util.Optional;

public class JwtTokenVerifier implements AuthTokenVerfier {
    private final ImmutableStrategyConfig strategy;
    private final JtiProvider jti;
    private final JWTVerifier verifier;

    public JwtTokenVerifier(final ImmutableStrategyConfig strategy, final JtiProvider jti,
                            final Algorithm algorithm) {
        this.strategy = strategy;
        this.jti = jti;

        this.verifier = JWT.require(algorithm).build();
    }

    public JwtTokenVerifier(final ImmutableStrategyConfig strategy, final Algorithm algorithm) {
        this.strategy = strategy;
        this.jti = null;

        this.verifier = JWT.require(algorithm).build();
    }

    Optional<DecodedJWT> verify(final String token) {
        try {
            return Optional.of(JWT.decode(token))
                    .map(verifier::verify)
                    .filter(this::verifyJti);
        } catch (final JWTVerificationException e) {
            return Optional.empty();
        }
    }

    private boolean verifyJti(final DecodedJWT decoded) {
        return !strategy.getUseJti() || jti.validate(decoded.getId());
    }

    @Override
    public Optional<String> verifyAccountToken(String token) {
        return verify(token)
                .map(Payload::getSubject);
    }
}