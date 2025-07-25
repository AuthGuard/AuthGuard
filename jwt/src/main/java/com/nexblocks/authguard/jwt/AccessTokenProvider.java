package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.crypto.TokenEncryptorAdapter;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

@ProvidesToken("accessToken")
public class AccessTokenProvider implements AuthProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenProvider.class);

    private static final String TOKEN_TYPE = "accessToken";

    private final TrackingSessionsService trackingSessionsService;
    private final AccountTokensRepository accountTokensRepository;
    private final JtiProvider jti;
    private final ServiceMapper serviceMapper;
    private final TokenEncryptorAdapter tokenEncryptor;

    private final Algorithm algorithm;
    private final JwtGenerator jwtGenerator;
    private final StrategyConfig strategy;
    private final Duration tokenTtl;
    private final Duration refreshTokenTtl;
    private final boolean encrypt;

    @Inject
    public AccessTokenProvider(final AccountTokensRepository accountTokensRepository,
                               final @Named("jwt") ConfigContext jwtConfigContext,
                               final @Named("accessToken") ConfigContext accessTokenConfigContext,
                               final JtiProvider jti,
                               final TokenEncryptorAdapter tokenEncryptor,
                               final ServiceMapper serviceMapper, final TrackingSessionsService trackingSessionsService) {
        this(trackingSessionsService, accountTokensRepository,
                jwtConfigContext.asConfigBean(JwtConfig.class),
                accessTokenConfigContext.asConfigBean(StrategyConfig.class),
                jti, tokenEncryptor, serviceMapper);
    }

    public AccessTokenProvider(final TrackingSessionsService trackingSessionsService,
                               final AccountTokensRepository accountTokensRepository,
                               final JwtConfig jwtConfig,
                               final StrategyConfig accessTokenConfig,
                               final JtiProvider jti,
                               final TokenEncryptorAdapter tokenEncryptor,
                               final ServiceMapper serviceMapper) {
        this.trackingSessionsService = trackingSessionsService;
        this.accountTokensRepository = accountTokensRepository;
        this.jti = jti;
        this.tokenEncryptor = tokenEncryptor;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());
        this.jwtGenerator = new JwtGenerator(jwtConfig);

        this.strategy = accessTokenConfig;
        this.serviceMapper = serviceMapper;
        this.tokenTtl = ConfigParser.parseDuration(strategy.getTokenLife());
        this.refreshTokenTtl = ConfigParser.parseDuration(strategy.getRefreshTokenLife());
        this.encrypt = jwtConfig.getEncryption() != null;
    }

    @Override
    public Uni<AuthResponseBO> generateToken(final AccountBO account, final TokenRestrictionsBO restrictions,
                                                           final TokenOptionsBO options) {
        if (!account.isActive()) {
            LOG.warn("Access token request for an inactive account. accountId={}, domain={}",
                    account.getId(), account.getDomain());

            throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_INACTIVE, "Account was deactivated");
        }

        LOG.debug("Access token request. accountId={}, domain={}", account.getId(), account.getDomain());

        JwtTokenBuilder tokenBuilder = generateAccessToken(account, restrictions, options);

        LOG.info("Generated access token. accountId={}, domain={}", account.getId(), account.getDomain());

        String signedToken = tokenBuilder.getBuilder().sign(algorithm);
        String finalToken = encryptIfNeeded(signedToken);
        String refreshToken = jwtGenerator.generateRandomRefreshToken();

        return trackingSessionsService.isSessionActive(options.getTrackingSession(), account.getDomain())
                .flatMap(isActive -> {
                    if (!isActive) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.SESSION_TERMINATED,
                                "Session is no longer active"));
                    }

                    return storeRefreshToken(account.getId(), refreshToken, restrictions, options);
                })
                .map(persisted -> {
                    LOG.info("Generated refresh token. accountId={}, domain={}, tokenId={}, expiresAt={}",
                            account.getId(), account.getDomain(), persisted.getId(), persisted.getExpiresAt());

                    return AuthResponseBO.builder()
                            .id(tokenBuilder.getId().orElse(""))
                            .type(TOKEN_TYPE)
                            .token(finalToken)
                            .refreshToken(refreshToken)
                            .entityType(EntityType.ACCOUNT)
                            .entityId(account.getId())
                            .validFor(tokenTtl.getSeconds())
                            .trackingSession(options.getTrackingSession())
                            .build();
                });
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Access tokens cannot be generated for an application");
    }

    @Override
    public Uni<AuthResponseBO> delete(final AuthRequestBO authRequest) {
        return deleteRefreshToken(authRequest.getToken())
                .map(opt -> opt
                        .map(accountToken -> AuthResponseBO.builder()
                                .type(TOKEN_TYPE)
                                .entityId(accountToken.getAssociatedAccountId())
                                .entityType(EntityType.ACCOUNT)
                                .refreshToken(authRequest.getToken())
                                .build())
                        .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid refresh token")));
    }

    private Uni<AccountTokenDO> storeRefreshToken(final long accountId, final String refreshToken,
                                                                final TokenRestrictionsBO tokenRestrictions,
                                                                final TokenOptions tokenOptions) {
        AccountTokenDO.AccountTokenDOBuilder<?, ?> accountToken = AccountTokenDO.builder()
                .id(ID.generate())
                .createdAt(Instant.now())
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(refreshTokenExpiry())
                .tokenRestrictions(serviceMapper.toDO(tokenRestrictions)); // Mapstruct already checks for null

        if (tokenOptions != null) {
            accountToken.sourceIp(tokenOptions.getSourceIp())
                    .trackingSession(tokenOptions.getTrackingSession())
                    .clientId(tokenOptions.getClientId())
                    .deviceId(tokenOptions.getDeviceId())
                    .sourceAuthType(tokenOptions.getSource())
                    .externalSessionId(tokenOptions.getExternalSessionId())
                    .trackingSession(tokenOptions.getTrackingSession())
                    .userAgent(tokenOptions.getUserAgent());
        }

        return accountTokensRepository.save(accountToken.build());
    }

    private Uni<Optional<AccountTokenDO>> deleteRefreshToken(final String refreshToken) {
        return accountTokensRepository.deleteToken(refreshToken);
    }

    private JwtTokenBuilder generateAccessToken(final AccountBO account, final TokenRestrictionsBO restrictions,
                                                final TokenOptionsBO options) {
        JwtTokenBuilder.Builder tokenBuilder = JwtTokenBuilder.builder();
        JWTCreator.Builder jwtBuilder = jwtGenerator.generateUnsignedToken(account, tokenTtl);

        if (strategy.useJti()) {
            final String id = jti.next();
            jwtBuilder.withJWTId(id);
            tokenBuilder.id(id);
        }

        if (strategy.includePermissions()) {
            jwtBuilder.withArrayClaim("permissions", JwtPermissionsMapper.map(account, restrictions));
        }

        if (strategy.includeExternalId()) {
            jwtBuilder.withClaim("eid", account.getExternalId());
        }

        if (strategy.includeRoles()) {
            jwtBuilder.withArrayClaim("roles", account.getRoles().toArray(new String[] {}));
        }

        if (strategy.includeVerification()) {
            if (account.getEmail() != null) {
                jwtBuilder.withClaim("emailVerified", account.getEmail().isVerified());
            }

            if (account.getPhoneNumber() != null) {
                jwtBuilder.withClaim("phoneVerified", account.getPhoneNumber().isVerified());
            }
        }

        if (options != null) {
            jwtBuilder.withClaim("sid", options.getTrackingSession());
            jwtBuilder.withClaim("source", options.getSource());
        }

        return tokenBuilder.builder(jwtBuilder).build();
    }

    private String encryptIfNeeded(final String token) {
        return this.encrypt
                ? tokenEncryptor.encryptAndEncode(token).get()
                : token;
    }

    private Instant refreshTokenExpiry() {
        return Instant.now().plus(refreshTokenTtl);
    }
}
