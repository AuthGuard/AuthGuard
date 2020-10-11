package com.authguard.service.exchange.apps;

import com.authguard.jwt.ApiTokenVerifier;
import com.authguard.jwt.JwtApiKeyProvider;
import com.authguard.service.exchange.ApiKeyExchange;
import com.authguard.service.exchange.KeyExchange;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;

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
    public TokensBO generateKey(final AppBO app) {
        return tokenProvider.generateToken(app);
    }

    @Override
    public CompletableFuture<Optional<String>> verifyAndGetAppId(final String apiKey) {
        return CompletableFuture.supplyAsync(() -> tokenVerifier.verifyAccountToken(apiKey));
    }
}
