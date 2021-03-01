package com.nexblocks.authguard.sessions.exchange;

import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokensBO;
import com.nexblocks.authguard.sessions.SessionVerifier;
import com.google.inject.Inject;
import io.vavr.control.Either;

@TokenExchange(from = "sessionToken", to = "accountId")
public class SessionToAccountId implements Exchange {
    private static final String TOKEN_TYPE = "account_id";

    private final SessionVerifier sessionVerifier;

    @Inject
    public SessionToAccountId(final SessionVerifier sessionVerifier) {
        this.sessionVerifier = sessionVerifier;
    }

    @Override
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        return sessionVerifier.verifyAccountToken(request.getToken())
                .map(accountId -> TokensBO.builder()
                        .type(TOKEN_TYPE)
                        .token(accountId)
                        .build());
    }
}
