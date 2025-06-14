package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.Session;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

import io.smallrye.mutiny.Uni;

@TokenExchange(from = "basic", to = "accessToken")
public class BasicToAccessToken implements Exchange {
    private final BasicAuthProvider basicAuth;
    private final AccessTokenProvider accessTokenProvider;

    @Inject
    public BasicToAccessToken(final BasicAuthProvider basicAuth, final AccessTokenProvider accessTokenProvider) {
        this.basicAuth = basicAuth;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return basicAuth.authenticateAndGetAccountSession(request)
                .flatMap(accountSession -> {
                    TokenOptionsBO options = getOptions(request, accountSession.getSession());

                    if (request.getRestrictions() == null) {
                        return accessTokenProvider.generateToken(accountSession.getAccount(), options);
                    } else {
                        return accessTokenProvider.generateToken(accountSession.getAccount(),
                                request.getRestrictions(), options);
                    }
                });
    }

    private TokenOptionsBO getOptions(final AuthRequestBO request,
                                      final Session session) {
        return TokenOptionsBO.builder()
                .source("basic")
                .userAgent(request.getUserAgent())
                .sourceIp(request.getSourceIp())
                .clientId(request.getClientId())
                .trackingSession(session.getSessionToken())
                .externalSessionId(request.getExternalSessionId())
                .deviceId(request.getDeviceId())
                .build();

    }
}
