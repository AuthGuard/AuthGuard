package com.nexblocks.authguard.sessions.exchange;

import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import com.nexblocks.authguard.sessions.SessionProvider;
import com.google.inject.Inject;

import io.smallrye.mutiny.Uni;

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
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        final TokenOptionsBO options = TokenOptionsBO.builder()
                .source("basic")
                .sourceIp(request.getSourceIp())
                .userAgent(request.getUserAgent())
                .build();

        return basicAuth.authenticateAndGetAccount(request)
                .flatMap(account -> sessionProvider.generateToken(account, options));
    }
}
