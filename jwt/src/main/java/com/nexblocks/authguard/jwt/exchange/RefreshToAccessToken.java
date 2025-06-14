package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

@TokenExchange(from = "refresh", to = "accessToken")
public class RefreshToAccessToken implements Exchange {
    private static final Logger LOG = LoggerFactory.getLogger(RefreshToAccessToken.class);

    private final AccountTokensRepository accountTokensRepository;
    private final AccountsService accountsService;
    private final AccessTokenProvider accessTokenProvider;
    private final JwtConfig jwtConfig;
    private final ServiceMapper serviceMapper;

    @Inject
    public RefreshToAccessToken(final AccountTokensRepository accountTokensRepository,
                                final AccountsService accountsService,
                                final AccessTokenProvider accessTokenProvider,
                                final @Named("jwt") ConfigContext jwtConfigContext,
                                final ServiceMapper serviceMapper) {
        this(accountTokensRepository, accountsService, accessTokenProvider,
                jwtConfigContext.asConfigBean(JwtConfig.class), serviceMapper);
    }

    public RefreshToAccessToken(final AccountTokensRepository accountTokensRepository,
                                final AccountsService accountsService,
                                final AccessTokenProvider accessTokenProvider,
                                final JwtConfig jwtConfig,
                                final ServiceMapper serviceMapper) {
        this.accountTokensRepository = accountTokensRepository;
        this.accountsService = accountsService;
        this.accessTokenProvider = accessTokenProvider;
        this.jwtConfig = jwtConfig;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return accountTokensRepository.getByToken(request.getToken())
                .flatMap(opt -> {
                    if (opt.isPresent()) {
                        return this.generateAndClear(opt.get(), request);
                    }

                    return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN,
                            "Invalid token"));
                });
    }

    private Uni<AuthResponseBO> generateAndClear(final AccountTokenDO accountToken,
                                                               final AuthRequest request) {
        return generate(accountToken, request)
                .map(response -> {
                    deleteRefreshToken(accountToken);
                    return response;
                });
    }

    private Uni<AuthResponseBO> generate(final AccountTokenDO accountToken,
                                                       final AuthRequest authRequest) {
        if (!validateExpirationDateTime(accountToken)) {
            ServiceAuthorizationException error =
                    new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Refresh token has expired",
                            EntityType.ACCOUNT, accountToken.getAssociatedAccountId());

            deleteRefreshToken(accountToken);

            return Uni.createFrom().failure(error);
        }

        Optional<String> invalidTokenValues = getInvalidTokenValues(accountToken, authRequest);

        if (invalidTokenValues.isPresent()) {
            ServiceAuthorizationException error =
                    new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, invalidTokenValues.get(),
                            EntityType.ACCOUNT, accountToken.getAssociatedAccountId());

            return Uni.createFrom().failure(error);
        }

        return generateNewTokens(accountToken);
    }

    private Uni<AuthResponseBO> generateNewTokens(final AccountTokenDO accountToken) {
        long accountId = accountToken.getAssociatedAccountId();
        TokenRestrictionsBO tokenRestrictions = serviceMapper.toBO(accountToken.getTokenRestrictions());

        TokenOptionsBO options = TokenOptionsBO.builder()
                .source(accountToken.getSourceAuthType())
                .userAgent(accountToken.getUserAgent())
                .sourceIp(accountToken.getSourceIp())
                .clientId(accountToken.getClientId())
                .externalSessionId(accountToken.getExternalSessionId())
                .trackingSession(accountToken.getTrackingSession())
                .deviceId(accountToken.getDeviceId())
                .build();

        return getAccount(accountId, accountToken)
                .flatMap(account -> accessTokenProvider.generateToken(account, tokenRestrictions, options));
    }

    private Uni<AccountBO> getAccount(final long accountId, final AccountTokenDO accountToken) {
        return accountsService.getByIdUnchecked(accountId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        deleteRefreshToken(accountToken);

                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                "Could not find account " + accountId));
                    }

                    return Uni.createFrom().item(opt.get());
                });
    }

    private boolean validateExpirationDateTime(final AccountTokenDO accountToken) {
        Instant now = Instant.now();

        return now.isBefore(accountToken.getExpiresAt());
    }

    private Optional<String> getInvalidTokenValues(final AccountTokenDO accountToken, AuthRequest authRequest) {
        if (jwtConfig.checkRefreshTokenOption()) {
            if (!Objects.equals(accountToken.getClientId(), authRequest.getClientId())) {
                LOG.warn("Request received with unexpected client ID. expected={}, request={}",
                        accountToken.getClientId(), authRequest);
                return Optional.of("Client ID value mismatch");
            }

            if (!Objects.equals(accountToken.getDeviceId(), authRequest.getDeviceId())) {
                LOG.warn("Request received with unexpected device ID. expected={}, request={}",
                        accountToken.getDeviceId(), authRequest);
                return Optional.of("Device ID value mismatch");
            }

            if (!Objects.equals(accountToken.getUserAgent(), authRequest.getUserAgent())) {
                LOG.warn("Request received with unexpected user agent. expected={}, request={}",
                        accountToken.getUserAgent(), authRequest);
                return Optional.of("User agent value mismatch");
            }

            if (!Objects.equals(accountToken.getExternalSessionId(), authRequest.getExternalSessionId())) {
                LOG.warn("Request received with unexpected external session ID. expected={}, request={}",
                        accountToken.getExternalSessionId(), authRequest);
                return Optional.of("External session ID value mismatch");
            }
        }

        if (jwtConfig.checkRefreshTokenRequestIp()) {
            if (!Objects.equals(accountToken.getSourceIp(), authRequest.getSourceIp())) {
                LOG.warn("Request received with unexpected source IP. expected={}, request={}",
                        accountToken.getSourceIp(), authRequest);
                return Optional.of("Source IP value mismatch");
            }
        }

        return Optional.empty();
    }

    private void deleteRefreshToken(final AccountTokenDO accountToken) {
        LOG.info("Deleting old refresh token. tokenId={}, accountId={}",
                accountToken.getId(), accountToken.getAssociatedAccountId());

        accountTokensRepository.deleteToken(accountToken.getToken())
                .onFailure()
                .invoke(e -> LOG.error("Failed to delete refresh token. tokenId={}, accountId={}",
                        accountToken.getId(), accountToken.getAssociatedAccountId(), e))
                .subscribe().with(deletedToken -> {
                    LOG.info("Deleted refresh token. tokenId={}, accountId={}",
                            accountToken.getId(), accountToken.getAssociatedAccountId());
                });
    }
}
