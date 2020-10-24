package com.authguard.jwt.exchange;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.jwt.AccessTokenProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Optional;

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
    public Optional<TokensBO> exchangeToken(final String refreshToken) {
        return accountTokensRepository.getByToken(refreshToken)
                .thenApply(optional -> optional
                        .filter(this::validateExpirationDateTime)
                        .map(AccountTokenDO::getAssociatedAccountId)
                        .map(this::generateTokenForAccount)
                        .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST,
                                "Non-existing or expired refresh token")))
                .thenApply(Optional::of)
                .join();
    }

    private TokensBO generateTokenForAccount(final String accountId) {
        return Optional.of(accountId)
                .map(this::getAccount)
                .map(accessTokenProvider::generateToken)
                .orElseThrow(IllegalStateException::new);
    }

    private AccountBO getAccount(final String accountId) {
        return accountsService.getById(accountId)
                .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                        "Could not find account " + accountId));
    }

    private boolean validateExpirationDateTime(final AccountTokenDO accountToken) {
        final ZonedDateTime now = ZonedDateTime.now();

        if (now.isAfter(accountToken.getExpiresAt())) {
            throw new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Refresh token " + accountToken.getExpiresAt()
                    + " for account " + accountToken.getAssociatedAccountId() + " has expired");
        }

        return true;
    }
}
