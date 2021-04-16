package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;

import java.util.List;
import java.util.Optional;

public interface ApiKeysService extends CrudService<ApiKeyBO> {
    ApiKeyBO generateApiKey(String appId);

    ApiKeyBO generateApiKey(AppBO app);

    List<ApiKeyBO> getByAppId(String appId);

    Optional<AppBO> validateApiKey(String key);
}
