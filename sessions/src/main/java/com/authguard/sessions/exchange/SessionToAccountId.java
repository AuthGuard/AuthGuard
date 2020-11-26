package com.authguard.sessions.exchange;

import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.TokensBO;
import com.authguard.sessions.SessionVerifier;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.util.Optional;

@TokenExchange(from = "session", to = "accountId")
public class SessionToAccountId implements Exchange {
    private final SessionVerifier sessionVerifier;

    @Inject
    public SessionToAccountId(final SessionVerifier sessionVerifier) {
        this.sessionVerifier = sessionVerifier;
    }

    @Override
    public Either<Exception, TokensBO> exchangeToken(final String sessionId) {
        return sessionVerifier.verifyAccountToken(sessionId)
                .map(accountId -> TokensBO.builder()
                        .type("accountId")
                        .token(accountId)
                        .build());
    }
}
