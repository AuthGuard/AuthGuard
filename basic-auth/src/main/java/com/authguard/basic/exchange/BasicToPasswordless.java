package com.authguard.basic.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokensBO;
import com.authguard.basic.passwordless.PasswordlessProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "passwordless")
public class BasicToPasswordless implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final PasswordlessProvider passwordlessProvider;

    @Inject
    public BasicToPasswordless(final BasicAuthProvider basicAuth, final PasswordlessProvider passwordlessProvider) {
        this.basicAuth = basicAuth;
        this.passwordlessProvider = passwordlessProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken) {
        return basicAuth.getAccount(basicToken)
                .map(passwordlessProvider::generateToken);
    }
}