package com.authguard.service;

import com.authguard.service.model.AppBO;

import java.util.Optional;

public interface ApiKeysService {
    String generateApiKey(String appId);

    String generateApiKey(AppBO app);

    Optional<AppBO> validateApiKey();
}
