package com.authguard.sessions.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokensBO;
import com.authguard.sessions.SessionProvider;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "basic", to = "session")
public class BasicToSession implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final SessionProvider sessionProvider;

    @Inject
    public BasicToSession(final BasicAuthProvider basicAuth, final SessionProvider sessionProvider) {
        this.basicAuth = basicAuth;
        this.sessionProvider = sessionProvider;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String basicToken) {
        return basicAuth.authenticateAndGetAccount(basicToken)
                .map(sessionProvider::generateToken);
    }
}
