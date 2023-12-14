package com.nexblocks.authguard.service.exchange;

import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.ClientBO;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApiKeyExchange {
    AuthResponseBO generateKey(AppBO app, Instant expiresAt);
    AuthResponseBO generateKey(ClientBO client, Instant expiresAt);

    CompletableFuture<Optional<Long>> verifyAndGetAppId(String apiKey);
    CompletableFuture<Optional<Long>> verifyAndGetClientId(String apiKey);
}
