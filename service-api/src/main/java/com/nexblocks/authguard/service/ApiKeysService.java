package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.ClientBO;

import java.time.Duration;
import java.util.List;
import io.smallrye.mutiny.Uni;

public interface ApiKeysService extends CrudService<ApiKeyBO> {
    Uni<ApiKeyBO> generateApiKey(long appId, String domain, String type, String name, Duration duration);
    Uni<ApiKeyBO> generateClientApiKey(long clientId, String domain, String type, String name, Duration duration);

    Uni<ApiKeyBO> generateApiKey(AppBO app, String type, String name, Duration duration);
    Uni<ApiKeyBO> generateClientApiKey(ClientBO client, String type, String name, Duration duration);

    Uni<List<ApiKeyBO>> getByAppId(long appId, String domain);

    Uni<AppBO> validateApiKey(String key, String domain, String type);

    Uni<ClientBO> validateClientApiKey(String key, String type);
}
