package com.authguard.service.impl.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.authguard.service.config.ConfigParser;
import com.google.inject.Inject;
import com.authguard.service.JtiProvider;
import com.authguard.service.JwtProvider;
import com.authguard.service.config.ImmutableJwtConfig;
import com.authguard.service.config.ImmutableStrategyConfig;
import com.authguard.service.model.*;

import java.util.Optional;

public class AccessTokenProvider implements JwtProvider {
    private final JtiProvider jti;
    private final Algorithm algorithm;
    private final TokenGenerator tokenGenerator;
    private final TokenVerifier tokenVerifier;
    private final ImmutableStrategyConfig strategy;

    @Inject
    public AccessTokenProvider(final ImmutableJwtConfig jwtConfig, final JtiProvider jti) {
        this.jti = jti;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getKey());
        this.tokenGenerator = new TokenGenerator(jwtConfig);
        this.strategy = jwtConfig.getStrategies().getAccessToken();
        this.tokenVerifier = new TokenVerifier(this.strategy, jti, algorithm);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final TokenBuilderBO tokenBuilder = generateAccessToken(account);
        final String token = tokenBuilder.getBuilder().sign(algorithm);
        final String refreshToken = tokenGenerator.generateRandomRefreshToken();

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

    @Override
    public Optional<DecodedJWT> validateToken(final String token) {
        return tokenVerifier.verify(token);
    }

    private TokenBuilderBO generateAccessToken(final AccountBO account) {
        final TokenBuilderBO.Builder tokenBuilder = TokenBuilderBO.builder();
        final JWTCreator.Builder jwtBuilder = tokenGenerator.generateUnsignedToken(account,
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
