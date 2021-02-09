package com.nexblocks.authguard.sessions.exchange;

import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokensBO;
import com.nexblocks.authguard.sessions.SessionProvider;
import com.google.inject.Inject;
import io.vavr.control.Either;

@TokenExchange(from = "basic", to = "sessionToken")
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
