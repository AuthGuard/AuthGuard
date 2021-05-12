package com.nexblocks.authguard.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import io.vavr.control.Either;

public class IdTokenVerifier {
    private final JwtTokenVerifier jwtTokenVerifier;

    @Inject
    public IdTokenVerifier(final @Named("jwt") ConfigContext jwtConfigContext,
                           final @Named("idToken") ConfigContext accessTokenConfigContext,
                           final JtiProvider jti) {
        this(jwtConfigContext.asConfigBean(JwtConfig.class), accessTokenConfigContext.asConfigBean(StrategyConfig.class),
                jti);
    }

    public IdTokenVerifier(final JwtConfig jwtConfig, final StrategyConfig strategy,
                           final JtiProvider jti) {
        final Algorithm algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());

        this.jwtTokenVerifier = new JwtTokenVerifier(strategy, jti, algorithm);
    }

    public Either<Exception, String> verify(final String token) {
        return jwtTokenVerifier.verifyAccountToken(token)
                .map(ignored -> token);
    }
}
