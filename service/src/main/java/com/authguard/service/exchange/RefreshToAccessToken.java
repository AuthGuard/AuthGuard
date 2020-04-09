package com.authguard.service.exchange;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AccountsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.jwt.AccessTokenProvider;
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
                        .orElseThrow(() -> new ServiceAuthorizationException("Non-existing or expired refresh token")))
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
                .orElseThrow(() -> new ServiceAuthorizationException("Could not find account " + accountId));
    }

    private boolean validateExpirationDateTime(final AccountTokenDO accountToken) {
        final ZonedDateTime now = ZonedDateTime.now();

        if (now.isAfter(accountToken.expiresAt())) {
            throw new ServiceAuthorizationException("Refresh token " + accountToken.expiresAt()
                    + " for account " + accountToken.getAssociatedAccountId() + " has expired");
        }

        return true;
    }
}
