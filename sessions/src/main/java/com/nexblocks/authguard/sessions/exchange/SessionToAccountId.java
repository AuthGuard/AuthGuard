package com.nexblocks.authguard.sessions.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.sessions.SessionVerifier;

import io.smallrye.mutiny.Uni;

@TokenExchange(from = "sessionToken", to = "accountId")
public class SessionToAccountId implements Exchange {
    private static final String TOKEN_TYPE = "account_id";

    private final SessionVerifier sessionVerifier;

    @Inject
    public SessionToAccountId(final SessionVerifier sessionVerifier) {
        this.sessionVerifier = sessionVerifier;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return sessionVerifier.verifyAccountTokenAsync(request)
                .map(token -> AuthResponseBO.builder()
                        .type(TOKEN_TYPE)
                        .token(token)
                        .build());
    }
}
