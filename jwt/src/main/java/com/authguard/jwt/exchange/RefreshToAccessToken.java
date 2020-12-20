package com.authguard.jwt.exchange;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.jwt.AccessTokenProvider;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.EntityType;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
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
                .map(this::generate)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid refresh token")));
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

        if (now.isAfter(accountToken.getExpiresAt())) {
            throw new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Refresh token " + accountToken.getToken()
                    + " has expired", EntityType.ACCOUNT, accountToken.getAssociatedAccountId());
        }

        return true;
    }
}
