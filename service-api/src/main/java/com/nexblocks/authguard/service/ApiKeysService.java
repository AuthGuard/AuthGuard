package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;

import java.util.List;
import java.util.Optional;

public interface ApiKeysService extends CrudService<ApiKeyBO> {
    ApiKeyBO generateApiKey(String appId, String type);

    ApiKeyBO generateApiKey(AppBO app, String type);

    List<ApiKeyBO> getByAppId(String appId);

    Optional<AppBO> validateApiKey(String key, String type);
}
