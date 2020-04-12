package com.authguard.service.exchange;

import com.authguard.service.model.TokensBO;
import com.authguard.service.sessions.SessionVerifier;
import com.google.inject.Inject;

import java.util.Optional;

@TokenExchange(from = "session", to = "accountId")
public class SessionToAccountId implements Exchange {
    private final SessionVerifier sessionVerifier;

    @Inject
    public SessionToAccountId(final SessionVerifier sessionVerifier) {
        this.sessionVerifier = sessionVerifier;
    }

    @Override
    public Optional<TokensBO> exchangeToken(final String sessionId) {
        return sessionVerifier.verifyAccountToken(sessionId)
                .map(accountId -> TokensBO.builder()
                        .type("accountId")
                        .token(accountId)
                        .build());
    }
}
