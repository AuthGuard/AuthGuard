package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;

import java.util.Optional;

public interface ApiKeysService {
    ApiKeyBO generateApiKey(String appId);

    ApiKeyBO generateApiKey(AppBO app);

    Optional<AppBO> validateApiKey(String key);
}
