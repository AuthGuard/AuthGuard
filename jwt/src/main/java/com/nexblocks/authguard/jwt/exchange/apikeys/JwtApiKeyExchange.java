package com.nexblocks.authguard.jwt.exchange.apikeys;

import com.nexblocks.authguard.jwt.ApiTokenVerifier;
import com.nexblocks.authguard.jwt.JwtApiKeyProvider;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.exchange.KeyExchange;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.google.inject.Inject;
import com.nexblocks.authguard.service.model.ClientBO;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@KeyExchange(keyType = "jwtApiKey")
public class JwtApiKeyExchange implements ApiKeyExchange {
    private final JwtApiKeyProvider tokenProvider;
    private final ApiTokenVerifier tokenVerifier;

    @Inject
    public JwtApiKeyExchange(final JwtApiKeyProvider tokenProvider,
                             final ApiTokenVerifier tokenVerifier) {
        this.tokenProvider = tokenProvider;
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public AuthResponseBO generateKey(final AppBO app, final Instant expiresAt) {
        return expiresAt == null ?
                tokenProvider.generateToken(app) :
                tokenProvider.generateToken(app, expiresAt);
    }

    @Override
    public AuthResponseBO generateKey(ClientBO client, Instant expiresAt) {
        return expiresAt == null ?
                tokenProvider.generateToken(client) :
                tokenProvider.generateToken(client, expiresAt);
    }

    @Override
    public CompletableFuture<Optional<Long>> verifyAndGetAppId(final String apiKey) {
        return CompletableFuture.completedFuture(Optional.of(tokenVerifier.verifyAccountToken(apiKey)));
    }

    @Override
    public CompletableFuture<Optional<Long>> verifyAndGetClientId(String apiKey) {
        return verifyAndGetAppId(apiKey);
    }
}
