package com.authguard.service.exchange;

import com.authguard.service.exchange.helpers.BasicAuth;
import com.authguard.service.model.TokensBO;
import com.authguard.service.passwordless.PasswordlessProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "passwordless")
public class BasicToPasswordless implements Exchange {
    private final BasicAuth basicAuth;
    private final PasswordlessProvider passwordlessProvider;

    @Inject
    public BasicToPasswordless(final BasicAuth basicAuth, final PasswordlessProvider passwordlessProvider) {
        this.basicAuth = basicAuth;
        this.passwordlessProvider = passwordlessProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken) {
        return basicAuth.getAccount(basicToken)
                .map(passwordlessProvider::generateToken);
    }
}
