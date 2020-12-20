package com.authguard.sessions.exchange;

import com.authguard.basic.BasicAuthProvider;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.TokensBO;
import com.authguard.sessions.SessionProvider;
import com.google.inject.Inject;
import io.vavr.control.Either;

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
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccount(request)
                .map(sessionProvider::generateToken);
    }
}
