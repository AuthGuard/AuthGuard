package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.model.EntityType;
import com.google.inject.Inject;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.TokensBO;
import com.google.inject.name.Named;

import java.time.Duration;

public class IdTokenProvider implements AuthProvider {
    private final Algorithm algorithm;
    private final JwtGenerator jwtGenerator;
    private final StrategyConfig strategy;
    private final Duration tokenTtl;

    @Inject
    public IdTokenProvider(final @Named("jwt") ConfigContext jwtConfigContext,
                           final @Named("idToken") ConfigContext idTokenConfigContext) {
        final JwtConfig jwtConfig = jwtConfigContext.asConfigBean(JwtConfig.class);

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(), jwtConfig.getPrivateKey());
        this.jwtGenerator = new JwtGenerator(jwtConfig);
        this.strategy = idTokenConfigContext.asConfigBean(StrategyConfig.class);
        this.tokenTtl = ConfigParser.parseDuration(strategy.getTokenLife());
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final JwtTokenBuilder tokenBuilder = generateIdToke(account);
        final String token = tokenBuilder.getBuilder().sign(algorithm);
        final String refreshToken = jwtGenerator.generateRandomRefreshToken();

        return TokensBO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("ID tokens cannot be generated for an application");
    }

    private JwtTokenBuilder generateIdToke(final AccountBO account) {
        final JWTCreator.Builder jwtBuilder = jwtGenerator.generateUnsignedToken(account, tokenTtl);

        if (account.getExternalId() != null) {
            jwtBuilder.withClaim("eid", account.getExternalId());
        }

        return JwtTokenBuilder.builder()
                .builder(jwtBuilder)
                .build();
    }
}
