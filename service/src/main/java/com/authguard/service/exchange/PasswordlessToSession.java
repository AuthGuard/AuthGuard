package com.authguard.service.exchange;

import com.authguard.service.AccountsService;
import com.authguard.service.model.TokensBO;
import com.authguard.basic.passwordless.PasswordlessVerifier;
import com.authguard.service.sessions.SessionProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "passwordless", to = "session")
public class PasswordlessToSession implements Exchange {
    private final AccountsService accountsService;
    private final PasswordlessVerifier passwordlessVerifier;
    private final SessionProvider sessionProvider;

    @Inject
    public PasswordlessToSession(final AccountsService accountsService,
                                 final PasswordlessVerifier passwordlessVerifier,
                                 final SessionProvider sessionProvider) {
        this.accountsService = accountsService;
        this.passwordlessVerifier = passwordlessVerifier;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String passwordlessToken) {
        return passwordlessVerifier.verifyAccountToken(passwordlessToken)
                .flatMap(accountsService::getById)
                .map(sessionProvider::generateToken);
    }
}
