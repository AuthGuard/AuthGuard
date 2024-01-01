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
        return basicAuth.authenticateAndGetAccount(request)
                .thenApply(account -> generateTokens(account, request.getRestrictions()));
    }

    private AuthResponseBO generateTokens(final AccountBO account, final TokenRestrictionsBO restrictions) {
        final TokenOptionsBO options = TokenOptionsBO.builder()
                .source("basic")
                .build();

        return openIdConnectTokenProvider.generateToken(account, restrictions, options);
    }
}
