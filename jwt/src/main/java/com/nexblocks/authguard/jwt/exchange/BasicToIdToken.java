package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.IdTokenProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.google.inject.Inject;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "basic", to = "idToken")
public class BasicToIdToken implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final IdTokenProvider idTokenProvider;

    @Inject
    public BasicToIdToken(final BasicAuthProvider basicAuth, final IdTokenProvider idTokenProvider) {
        this.basicAuth = basicAuth;
        this.idTokenProvider = idTokenProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccount(request)
                .thenCompose(idTokenProvider::generateToken);
    }
}
