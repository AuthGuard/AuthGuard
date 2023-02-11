package com.nexblocks.authguard.service.exchange;

import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApiKeyExchange {
    AuthResponseBO generateKey(AppBO app, Instant expiresAt);

    CompletableFuture<Optional<String>> verifyAndGetAppId(String apiKey);
}
