package com.authguard.service.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.authguard.service.config.ConfigParser;
import com.google.inject.Inject;
import com.authguard.service.AuthProvider;
import com.authguard.service.config.ImmutableJwtConfig;
import com.authguard.service.config.ImmutableStrategyConfig;
import com.authguard.service.model.*;

public class AccessTokenProvider implements AuthProvider {
    private final JtiProvider jti;
    private final Algorithm algorithm;
    private final JwtGenerator jwtGenerator;
    private final ImmutableStrategyConfig strategy;

    @Inject
    public AccessTokenProvider(final ImmutableJwtConfig jwtConfig, final JtiProvider jti) {
        this.jti = jti;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getKey());
        this.jwtGenerator = new JwtGenerator(jwtConfig);
        this.strategy = jwtConfig.getStrategies().getAccessToken();
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final TokenBuilderBO tokenBuilder = generateAccessToken(account);
        final String token = tokenBuilder.getBuilder().sign(algorithm);
        final String refreshToken = jwtGenerator.generateRandomRefreshToken();

        return TokensBO.builder()
                .id(tokenBuilder.getId().orElse(null))
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Access tokens cannot be generated for an application");
    }

    private TokenBuilderBO generateAccessToken(final AccountBO account) {
        final TokenBuilderBO.Builder tokenBuilder = TokenBuilderBO.builder();
        final JWTCreator.Builder jwtBuilder = jwtGenerator.generateUnsignedToken(account,
                ConfigParser.parseDuration(strategy.getTokenLife()));

        if (strategy.getUseJti()) {
            final String id = jti.next();
            jwtBuilder.withJWTId(id);
            tokenBuilder.id(id);
        }

        if (strategy.getIncludePermissions()) {
            jwtBuilder.withArrayClaim("permissions", account.getPermissions().stream()
                    .map(this::permissionToString).toArray(String[]::new));
        }

        if (strategy.getIncludeScopes()) {
            jwtBuilder.withArrayClaim("scopes", account.getScopes().toArray(new String[0]));
        }

        return tokenBuilder.builder(jwtBuilder).build();
    }

    private String permissionToString(final PermissionBO permission) {
        return permission.getGroup() + "." + permission.getName();
    }

}
