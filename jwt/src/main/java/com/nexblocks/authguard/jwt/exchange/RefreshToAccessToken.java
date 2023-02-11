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

import java.time.Instant;

@TokenExchange(from = "refresh", to = "accessToken")
public class RefreshToAccessToken implements Exchange {
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
        final Either<Exception, AuthResponseBO> result = generate(accountToken);

        /*
         * The refresh token cannot be reused, so we need to remove it.
         */
        accountTokensRepository.deleteToken(accountToken.getToken());

        return result;
    }

    private Either<Exception, AuthResponseBO> generate(final AccountTokenDO accountToken) {
        if (!validateExpirationDateTime(accountToken)) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Refresh token has expired",
                    EntityType.ACCOUNT, accountToken.getAssociatedAccountId()));
        }

        return generateNewTokens(accountToken);
    }

    private Either<Exception, AuthResponseBO> generateNewTokens(final AccountTokenDO accountToken) {
        final String accountId = accountToken.getAssociatedAccountId();
        final TokenRestrictionsBO tokenRestrictions = serviceMapper.toBO(accountToken.getTokenRestrictions());

        return getAccount(accountId).map(account -> accessTokenProvider.generateToken(account, tokenRestrictions));
    }

    private Either<Exception, AccountBO> getAccount(final String accountId) {
        return accountsService.getById(accountId)
                .<Either<Exception, AccountBO>>map(Either::right)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Could not find account " + accountId)));
    }

    private boolean validateExpirationDateTime(final AccountTokenDO accountToken) {
        final Instant now = Instant.now();

        return now.isBefore(accountToken.getExpiresAt());
    }
}
