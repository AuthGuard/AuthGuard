package com.authguard.service.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.authguard.service.config.ConfigParser;
import com.google.inject.Inject;
import com.authguard.service.AuthProvider;
import com.authguard.service.config.ImmutableJwtConfig;
import com.authguard.service.config.ImmutableStrategyConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokenBuilderBO;
import com.authguard.service.model.TokensBO;

import java.util.Optional;

public class IdTokenProvider implements AuthProvider {
    private final Algorithm algorithm;
    private final TokenGenerator tokenGenerator;
    private final TokenVerifier tokenVerifier;
    private final ImmutableStrategyConfig strategy;

    @Inject
    public IdTokenProvider(final ImmutableJwtConfig jwtConfig) {
        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getKey());
        this.tokenGenerator = new TokenGenerator(jwtConfig);
        this.strategy = jwtConfig.getStrategies().getIdToken();

        this.tokenVerifier = new TokenVerifier(this.strategy, algorithm);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final TokenBuilderBO tokenBuilder = generateIdToke(account);
        final String token = tokenBuilder.getBuilder().sign(algorithm);
        final String refreshToken = tokenGenerator.generateRandomRefreshToken();

        return TokensBO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("ID tokens cannot be generated for an application");
    }

    @Override
    public Optional<DecodedJWT> validateToken(final String token) {
        return tokenVerifier.verify(token);
    }

    private TokenBuilderBO generateIdToke(final AccountBO account) {
        final JWTCreator.Builder jwtBuilder = tokenGenerator
                .generateUnsignedToken(account, ConfigParser.parseDuration(strategy.getTokenLife()));

        return TokenBuilderBO.builder()
                .builder(jwtBuilder)
                .build();
    }
}
