package com.authguard.service.exchange;

import com.authguard.service.exchange.helpers.BasicAuth;
import com.authguard.service.model.TokensBO;
import com.authguard.service.sessions.SessionProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "session")
public class BasicToSession implements Exchange {
    private final BasicAuth basicAuth;
    private final SessionProvider sessionProvider;

    @Inject
    public BasicToSession(final BasicAuth basicAuth, final SessionProvider sessionProvider) {
        this.basicAuth = basicAuth;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken) {
        return basicAuth.authenticateAndGetAccount(basicToken)
                .map(sessionProvider::generateToken);
    }
}
