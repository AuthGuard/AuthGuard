package com.nexblocks.authguard.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.smallrye.mutiny.Uni;

public class ApiTokenVerifier implements AuthVerifier {
    private final JwtTokenVerifier jwtVerifier;

    @Inject
    public ApiTokenVerifier(final JtiProvider jtiProvider, final @Named("jwt") ConfigContext configContext) {
        final JwtConfig jwtConfig = configContext.asConfigBean(JwtConfig.class);
        final StrategyConfig strategy = StrategyConfig.builder().useJti(true).build();
        final Algorithm algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());

        this.jwtVerifier = new JwtTokenVerifier(strategy, jtiProvider, algorithm);
    }

    @Override
    public Uni<Long> verifyAccountToken(final String token) {
        return jwtVerifier.verifyAccountToken(token);
    }
}
