package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@TokenExchange(from = "refresh", to = "accessToken")
public class RefreshToAccessToken implements Exchange {
    private static final Logger LOG = LoggerFactory.getLogger(RefreshToAccessToken.class);

    private final AccountTokensRepository accountTokensRepository;
    private final AccountsService accountsService;
    private final AccessTokenProvider accessTokenProvider;
    private final ServiceMapper serviceMapper;

    @Inject
    public RefreshToAccessToken(final AccountTokensRepository accountTokensRepository,
                                final AccountsService accountsService,
                                final AccessTokenProvider accessTokenProvider,
                                final ServiceMapper serviceMapper) {
        this.accountTokensRepository = accountTokensRepository;
        this.accountsService = accountsService;
        this.accessTokenProvider = accessTokenProvider;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Either<Exception, AuthResponseBO> exchange(final AuthRequestBO request) {
        return accountTokensRepository.getByToken(request.getToken())
                .join()
                .map(this::generateAndClear)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid refresh token")));
    }

    private Either<Exception, AuthResponseBO> generateAndClear(final AccountTokenDO accountToken) {
        return generate(accountToken)
                .peek(response -> deleteRefreshToken(accountToken));
    }

    private Either<Exception, AuthResponseBO> generate(final AccountTokenDO accountToken) {
        if (!validateExpirationDateTime(accountToken)) {
            final ServiceAuthorizationException error = new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Refresh token has expired",
                    EntityType.ACCOUNT, accountToken.getAssociatedAccountId());

            deleteRefreshToken(accountToken);

            return Either.left(error);
        }

        return generateNewTokens(accountToken);
    }

    private Either<Exception, AuthResponseBO> generateNewTokens(final AccountTokenDO accountToken) {
        final String accountId = accountToken.getAssociatedAccountId();
        final TokenRestrictionsBO tokenRestrictions = serviceMapper.toBO(accountToken.getTokenRestrictions());

        return getAccount(accountId, accountToken).map(account -> accessTokenProvider.generateToken(account, tokenRestrictions));
    }

    private Either<Exception, AccountBO> getAccount(final String accountId, final AccountTokenDO accountToken) {
        return accountsService.getById(accountId)
                .<Either<Exception, AccountBO>>map(Either::right)
                .orElseGet(() -> {
                    deleteRefreshToken(accountToken);

                    return Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                            "Could not find account " + accountId));
                });
    }

    private boolean validateExpirationDateTime(final AccountTokenDO accountToken) {
        final Instant now = Instant.now();

        return now.isBefore(accountToken.getExpiresAt());
    }

    private void deleteRefreshToken(final AccountTokenDO accountToken) {
        LOG.info("Deleting old refresh token. tokenId={}, accountId={}",
                accountToken.getId(), accountToken.getAssociatedAccountId());

        accountTokensRepository.deleteToken(accountToken.getToken());
    }
}
