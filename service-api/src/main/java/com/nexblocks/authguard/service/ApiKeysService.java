package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.ClientBO;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ApiKeysService extends CrudService<ApiKeyBO> {
    CompletableFuture<ApiKeyBO> generateApiKey(long appId, String domain, String type, String name, Duration duration);
    CompletableFuture<ApiKeyBO> generateClientApiKey(long clientId, String domain, String type, String name, Duration duration);

    CompletableFuture<ApiKeyBO> generateApiKey(AppBO app, String type, String name, Duration duration);
    CompletableFuture<ApiKeyBO> generateClientApiKey(ClientBO client, String type, String name, Duration duration);

    CompletableFuture<List<ApiKeyBO>> getByAppId(long appId, String domain);

    CompletableFuture<AppBO> validateApiKey(String key, String domain, String type);

    CompletableFuture<ClientBO> validateClientApiKey(String key, String type);
}
