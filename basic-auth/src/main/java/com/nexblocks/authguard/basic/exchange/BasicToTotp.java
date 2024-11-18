package com.nexblocks.authguard.basic.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.basic.totp.TotpProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.TokenOptionsMapper;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "basic", to = "totp")
public class BasicToTotp implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final TotpProvider otpProvider;

    @Inject
    public BasicToTotp(final BasicAuthProvider basicAuth, final TotpProvider otpProvider) {
        this.basicAuth = basicAuth;
        this.otpProvider = otpProvider;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccountSession(request)
                .thenCompose(accountSession -> {
                    TokenOptionsBO tokenOptions = TokenOptionsMapper.fromAuthRequest(request)
                            .source("basic")
                            .trackingSession(accountSession.getSession().getSessionToken())
                            .build();

                    return otpProvider.generateToken(accountSession.getAccount(), tokenOptions);
                });
    }
}
