package com.nexblocks.authguard.jwt.exchange.apikeys;

import com.nexblocks.authguard.jwt.ApiTokenVerifier;
import com.nexblocks.authguard.jwt.JwtApiKeyProvider;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.exchange.KeyExchange;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
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
    public AuthResponseBO generateKey(final AppBO app) {
        return tokenProvider.generateToken(app);
    }

    @Override
    public CompletableFuture<Optional<String>> verifyAndGetAppId(final String apiKey) {
        return CompletableFuture.supplyAsync(() ->
                tokenVerifier.verifyAccountToken(apiKey)
                        .map(Optional::of)
                        .getOrElseThrow(e -> {
                            if (RuntimeException.class.isAssignableFrom(e.getClass())) {
                                return (RuntimeException) e;
                            }

                            return new RuntimeException(e);
                        }));
    }
}
