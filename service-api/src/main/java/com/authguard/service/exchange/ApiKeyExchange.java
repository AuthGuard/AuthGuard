package com.authguard.service.exchange;

import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApiKeyExchange {
    TokensBO generateKey(AppBO app);

    CompletableFuture<Optional<String>> verifyAndGetAppId(String apiKey);
}
