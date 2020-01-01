package org.auther.service;

import org.auther.service.model.AppBO;

import java.util.Optional;

public interface ApiKeysService {
    String generateApiKey(String appId);

    String generateApiKey(AppBO app);

    Optional<AppBO> validateApiKey();
}
