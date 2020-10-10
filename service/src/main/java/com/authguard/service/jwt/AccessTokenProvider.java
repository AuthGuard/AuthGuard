package com.authguard.service.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.authguard.config.ConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.config.JwtConfig;
import com.authguard.service.config.StrategyConfig;
import com.google.inject.Inject;
import com.authguard.service.auth.AuthProvider;
import com.authguard.service.model.*;
import com.google.inject.name.Named;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class AccessTokenProvider implements AuthProvider {
    private final AccountTokensRepository accountTokensRepository;
    private final JtiProvider jti;
    private final Algorithm algorithm;
    private final JwtGenerator jwtGenerator;
    private final StrategyConfig strategy;

    @Inject
    public AccessTokenProvider(final AccountTokensRepository accountTokensRepository,
                               final @Named("jwt") ConfigContext jwtConfigContext,
                               final @Named("accessToken") ConfigContext accessTokenConfigContext,
                               final JtiProvider jti) {
        this.accountTokensRepository = accountTokensRepository;
        this.jti = jti;

        final JwtConfig jwtConfig = jwtConfigContext.asConfigBean(JwtConfig.class);

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());
        this.jwtGenerator = new JwtGenerator(jwtConfig);

        this.strategy = accessTokenConfigContext.asConfigBean(StrategyConfig.class);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        return generateToken(account, null);
    }

    @Override
    public TokensBO generateToken(final AccountBO account, final TokenRestrictionsBO restrictions) {
        final JwtTokenBuilder tokenBuilder = generateAccessToken(account, restrictions);
        final String token = tokenBuilder.getBuilder().sign(algorithm);
        final String refreshToken = jwtGenerator.generateRandomRefreshToken();

        storeRefreshToken(account.getId(), refreshToken);

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

    private void storeRefreshToken(final String accountId, final String refreshToken) {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(UUID.randomUUID().toString())
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(refreshTokenExpiry())
                .build();

        accountTokensRepository.save(accountToken)
                .join();
    }

    private JwtTokenBuilder generateAccessToken(final AccountBO account, final TokenRestrictionsBO restrictions) {
        final JwtTokenBuilder.Builder tokenBuilder = JwtTokenBuilder.builder();
        final JWTCreator.Builder jwtBuilder = jwtGenerator.generateUnsignedToken(account,
                ConfigParser.parseDuration(strategy.getTokenLife()));

        if (strategy.useJti()) {
            final String id = jti.next();
            jwtBuilder.withJWTId(id);
            tokenBuilder.id(id);
        }

        if (strategy.includePermissions()) {
            jwtBuilder.withArrayClaim("permissions", jwtPermissions(account, restrictions));
        }

        if (strategy.includeScopes()) {
            jwtBuilder.withArrayClaim("scopes", jwtScopes(account, restrictions));
        }

        if (strategy.includeExternalId()) {
            jwtBuilder.withClaim("eid", account.getExternalId());
        }

        return tokenBuilder.builder(jwtBuilder).build();
    }

    private String permissionToString(final PermissionBO permission) {
        return permission.getGroup() + "." + permission.getName();
    }

    private ZonedDateTime refreshTokenExpiry() {
        return ZonedDateTime.now()
                .plus(ConfigParser.parseDuration(strategy.getRefreshTokenLife()));
    }

    private String[] jwtPermissions(final AccountBO account, final TokenRestrictionsBO restrictions) {
        Stream<String> mappedPermissions =  account.getPermissions().stream()
                .map(this::permissionToString);

        if (restrictions != null && !restrictions.getPermissions().isEmpty()) {
            mappedPermissions = mappedPermissions.filter(restrictions.getPermissions()::contains);
        }

        return mappedPermissions.toArray(String[]::new);
    }

    private String[] jwtScopes(final AccountBO account, final TokenRestrictionsBO restrictions) {
        if (restrictions == null || restrictions.getScopes().isEmpty()) {
            return account.getScopes().toArray(new String[0]);
        }

        return account.getScopes().stream()
                .filter(restrictions.getScopes()::contains)
                .toArray(String[]::new);
    }
}
