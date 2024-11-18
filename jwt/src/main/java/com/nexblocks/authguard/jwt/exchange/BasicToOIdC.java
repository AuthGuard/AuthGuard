package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.OpenIdConnectTokenProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.*;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "basic", to = "oidc")
public class BasicToOIdC implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final OpenIdConnectTokenProvider openIdConnectTokenProvider;

    @Inject
    public BasicToOIdC(final BasicAuthProvider basicAuth,
                       final OpenIdConnectTokenProvider openIdConnectTokenProvider) {
        this.basicAuth = basicAuth;
        this.openIdConnectTokenProvider = openIdConnectTokenProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccountSession(request)
                .thenCompose(accountSession -> generateTokens(accountSession, request.getRestrictions()));
    }

    private CompletableFuture<AuthResponseBO> generateTokens(final AccountSession accountSession,
                                                             final TokenRestrictionsBO restrictions) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("basic")
                .trackingSession(accountSession.getSession().getSessionToken())
                .build();

        return openIdConnectTokenProvider.generateToken(accountSession.getAccount(), restrictions, options);
    }
}
