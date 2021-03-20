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
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.TokensBO;
import io.vavr.control.Either;

import java.time.ZonedDateTime;

@TokenExchange(from = "refresh", to = "accessToken")
public class RefreshToAccessToken implements Exchange {
    private final AccountTokensRepository accountTokensRepository;
    private final AccountsService accountsService;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public RefreshToAccessToken(final AccountTokensRepository accountTokensRepository,
                                final AccountsService accountsService,
                                final AccessTokenProvider accessTokenProvider) {
        this.accountTokensRepository = accountTokensRepository;
        this.accountsService = accountsService;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return accountTokensRepository.getByToken(request.getToken())
                .join()
                .map(this::generateAndClear)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid refresh token")));
    }

    private Either<Exception, TokensBO> generateAndClear(final AccountTokenDO accountToken) {
        final Either<Exception, TokensBO> result = generate(accountToken);

        /*
         * The refresh token cannot be reused, so we need to remove it.
         */
        accountTokensRepository.deleteToken(accountToken.getToken());

        return result;
    }

    private Either<Exception, TokensBO> generate(final AccountTokenDO accountToken) {
        if (!validateExpirationDateTime(accountToken)) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Refresh token has expired",
                    EntityType.ACCOUNT, accountToken.getAssociatedAccountId()));
        }

        return generateTokenForAccount(accountToken.getAssociatedAccountId());
    }

    private Either<Exception, TokensBO> generateTokenForAccount(final String accountId) {
        return getAccount(accountId).map(accessTokenProvider::generateToken);
    }

    private Either<Exception, AccountBO> getAccount(final String accountId) {
        return accountsService.getById(accountId)
                .<Either<Exception, AccountBO>>map(Either::right)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Could not find account " + accountId)));
    }

    private boolean validateExpirationDateTime(final AccountTokenDO accountToken) {
        final ZonedDateTime now = ZonedDateTime.now();

        return now.isBefore(accountToken.getExpiresAt());
    }
}
