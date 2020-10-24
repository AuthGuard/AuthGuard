package com.authguard.jwt.exchange;

import com.authguard.service.AccountsService;
import com.authguard.jwt.AccessTokenProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokensBO;
import com.authguard.basic.passwordless.PasswordlessVerifier;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "passwordless", to = "accessToken")
public class PasswordlessToAccessToken implements Exchange {
    private final AccountsService accountsService;
    private final PasswordlessVerifier passwordlessVerifier;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public PasswordlessToAccessToken(final AccountsService accountsService,
                                     final PasswordlessVerifier passwordlessVerifier,
                                     final AccessTokenProvider accessTokenProvider) {
        this.accountsService = accountsService;
        this.passwordlessVerifier = passwordlessVerifier;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String passwordlessToken) {
        return passwordlessVerifier.verifyAccountToken(passwordlessToken)
                .flatMap(accountsService::getById)
                .map(accessTokenProvider::generateToken);
    }
}
