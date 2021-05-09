package com.nexblocks.authguard.service.exchange;

import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApiKeyExchange {
    AuthResponseBO generateKey(AppBO app);

    CompletableFuture<Optional<String>> verifyAndGetAppId(String apiKey);
}
