package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.persistence.ApiKeysRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.TokensBO;
import com.nexblocks.authguard.service.util.ID;

import java.util.Optional;

public class ApiKeysServiceImpl implements ApiKeysService {
    private static final String API_KEYS_CHANNELS = "api_keys";

    private final ApplicationsService applicationsService;
    private final ApiKeyExchange apiKeyExchange;
    private final PersistenceService<ApiKeyBO, ApiKeyDO, ApiKeysRepository> persistenceService;

    @Inject
    public ApiKeysServiceImpl(final ApplicationsService applicationsService,
                              final ApiKeyExchange apiKeyExchange,
                              final ApiKeysRepository keysRepository,
                              final MessageBus messageBus,
                              final ServiceMapper serviceMapper) {
        this.applicationsService = applicationsService;
        this.apiKeyExchange = apiKeyExchange;

        this.persistenceService = new PersistenceService<>(keysRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, API_KEYS_CHANNELS);
    }

    @Override
    public ApiKeyBO generateApiKey(final String appId) {
        final AppBO app = applicationsService.getById(appId)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST,
                        "No app with ID " + appId + " found"));

        return generateApiKey(app);
    }

    @Override
    public ApiKeyBO generateApiKey(final AppBO app) {
        final TokensBO token = apiKeyExchange.generateKey(app);
        final ApiKeyBO apiKey = ApiKeyBO.builder()
                .id(ID.generate())
                .appId(app.getId())
                .key((String) token.getToken())
                .build();

        return persistenceService.create(apiKey);
    }

    @Override
    public Optional<AppBO> validateApiKey(final String key) {
        return apiKeyExchange.verifyAndGetAppId(key)
                .thenApply(optional -> optional.flatMap(applicationsService::getById))
                .join();

    }
}
