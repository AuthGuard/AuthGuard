package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@ProvidesToken("accessToken")
public class AccessTokenProvider implements AuthProvider {
    private static final String TOKEN_TYPE = "access_token";

    private final AccountTokensRepository accountTokensRepository;
    private final JtiProvider jti;
    private final Algorithm algorithm;
    private final JwtGenerator jwtGenerator;
    private final StrategyConfig strategy;
    private final Duration tokenTtl;
    private final Duration refreshTokenTtl;

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
        this.tokenTtl = ConfigParser.parseDuration(strategy.getTokenLife());
        this.refreshTokenTtl = ConfigParser.parseDuration(strategy.getRefreshTokenLife());
    }

    public AccessTokenProvider(final AccountTokensRepository accountTokensRepository,
                               final JwtConfig jwtConfig,
                               final StrategyConfig accessTokenConfig,
                               final JtiProvider jti) {
        this.accountTokensRepository = accountTokensRepository;
        this.jti = jti;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());
        this.jwtGenerator = new JwtGenerator(jwtConfig);

        this.strategy = accessTokenConfig;
        this.tokenTtl = ConfigParser.parseDuration(strategy.getTokenLife());
        this.refreshTokenTtl = ConfigParser.parseDuration(strategy.getRefreshTokenLife());
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
                .type(TOKEN_TYPE)
                .token(token)
                .refreshToken(refreshToken)
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Access tokens cannot be generated for an application");
    }

    @Override
    public TokensBO delete(final AuthRequestBO authRequest) {
        return deleteRefreshToken(authRequest.getToken())
                .map(accountToken -> TokensBO.builder()
                        .type(TOKEN_TYPE)
                        .entityId(accountToken.getAssociatedAccountId())
                        .entityType(EntityType.ACCOUNT)
                        .refreshToken(authRequest.getToken())
                        .build())
                .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid refresh token"));
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

    private Optional<AccountTokenDO> deleteRefreshToken(final String refreshToken) {
        return accountTokensRepository.deleteToken(refreshToken).join();
    }

    private JwtTokenBuilder generateAccessToken(final AccountBO account, final TokenRestrictionsBO restrictions) {
        final JwtTokenBuilder.Builder tokenBuilder = JwtTokenBuilder.builder();
        final JWTCreator.Builder jwtBuilder = jwtGenerator.generateUnsignedToken(account, tokenTtl);

        if (strategy.useJti()) {
            final String id = jti.next();
            jwtBuilder.withJWTId(id);
            tokenBuilder.id(id);
        }

        if (strategy.includePermissions()) {
            jwtBuilder.withArrayClaim("permissions", jwtPermissions(account, restrictions));
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
        return ZonedDateTime.now().plus(refreshTokenTtl);
    }

    private String[] jwtPermissions(final AccountBO account, final TokenRestrictionsBO restrictions) {
        Stream<String> mappedPermissions =  account.getPermissions().stream()
                .map(this::permissionToString);

        if (restrictions != null && !restrictions.getPermissions().isEmpty()) {
            mappedPermissions = mappedPermissions.filter(restrictions.getPermissions()::contains);
        }

        return mappedPermissions.toArray(String[]::new);
    }
}
