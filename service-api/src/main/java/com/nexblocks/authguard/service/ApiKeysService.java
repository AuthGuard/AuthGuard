package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.ClientBO;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ApiKeysService extends CrudService<ApiKeyBO> {
    CompletableFuture<ApiKeyBO> generateApiKey(long appId, String type, Duration duration);
    CompletableFuture<ApiKeyBO> generateClientApiKey(long clientId, String type, Duration duration);

    CompletableFuture<ApiKeyBO> generateApiKey(AppBO app, String type, Duration duration);
    CompletableFuture<ApiKeyBO> generateClientApiKey(ClientBO client, String type, Duration duration);

    CompletableFuture<List<ApiKeyBO>> getByAppId(long appId);

    CompletableFuture<AppBO> validateApiKey(String key, String type);

    CompletableFuture<ClientBO> validateClientApiKey(String key, String type);
}
