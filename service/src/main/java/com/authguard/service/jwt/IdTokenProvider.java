package com.authguard.service.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.authguard.config.ConfigContext;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.config.StrategyConfig;
import com.google.inject.Inject;
import com.authguard.service.AuthProvider;
import com.authguard.service.config.JwtConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokenBuilderBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.name.Named;

public class IdTokenProvider implements AuthProvider {
    private final Algorithm algorithm;
    private final JwtGenerator jwtGenerator;
    private final StrategyConfig strategy;

    @Inject
    public IdTokenProvider(final @Named("jwt") ConfigContext jwtConfigContext,
                           final @Named("idToken") ConfigContext idTokenConfigContext) {
        final JwtConfig jwtConfig = jwtConfigContext.asConfigBean(JwtConfig.class);

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getKey());
        this.jwtGenerator = new JwtGenerator(jwtConfig);
        this.strategy = idTokenConfigContext.asConfigBean(StrategyConfig.class);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final TokenBuilderBO tokenBuilder = generateIdToke(account);
        final String token = tokenBuilder.getBuilder().sign(algorithm);
        final String refreshToken = jwtGenerator.generateRandomRefreshToken();

        return TokensBO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("ID tokens cannot be generated for an application");
    }

    private TokenBuilderBO generateIdToke(final AccountBO account) {
        final JWTCreator.Builder jwtBuilder = jwtGenerator
                .generateUnsignedToken(account, ConfigParser.parseDuration(strategy.getTokenLife()));

        if (account.getExternalId() != null) {
            jwtBuilder.withClaim("eid", account.getExternalId());
        }

        return TokenBuilderBO.builder()
                .builder(jwtBuilder)
                .build();
    }
}
