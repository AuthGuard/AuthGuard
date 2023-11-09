package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "basic", to = "authorizationCode")
public class BasicToAuthorizationCode implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final AuthorizationCodeProvider authorizationCodeProvider;

    @Inject
    public BasicToAuthorizationCode(final BasicAuthProvider basicAuth, final AuthorizationCodeProvider authorizationCodeProvider) {
        this.basicAuth = basicAuth;
        this.authorizationCodeProvider = authorizationCodeProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("basic")
                .userAgent(request.getUserAgent())
                .sourceIp(request.getSourceIp())
                .clientId(request.getClientId())
                .externalSessionId(request.getExternalSessionId())
                .deviceId(request.getDeviceId())
                .extraParameters(request.getExtraParameters())
                .build();
        return basicAuth.authenticateAndGetAccount(request)
                .thenCompose(account -> authorizationCodeProvider.generateToken(account, request.getRestrictions(), options));
    }
}
