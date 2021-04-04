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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApiKeysServiceImpl implements ApiKeysService {
    private static final String API_KEYS_CHANNELS = "api_keys";

    private final ApplicationsService applicationsService;
    private final ApiKeyExchange apiKeyExchange;
    private final ApiKeysRepository apiKeysRepository;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<ApiKeyBO, ApiKeyDO, ApiKeysRepository> persistenceService;

    @Inject
    public ApiKeysServiceImpl(final ApplicationsService applicationsService,
                              final ApiKeyExchange apiKeyExchange,
                              final ApiKeysRepository apiKeysRepository,
                              final MessageBus messageBus,
                              final ServiceMapper serviceMapper) {
        this.applicationsService = applicationsService;
        this.apiKeyExchange = apiKeyExchange;
        this.apiKeysRepository = apiKeysRepository;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(apiKeysRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, API_KEYS_CHANNELS);
    }

    @Override
    public ApiKeyBO create(final ApiKeyBO apiKey) {
        return persistenceService.create(apiKey);
    }

    @Override
    public Optional<ApiKeyBO> getById(final String apiKeyId) {
        return persistenceService.getById(apiKeyId);
    }

    @Override
    public Optional<ApiKeyBO> update(final ApiKeyBO entity) {
        throw new UnsupportedOperationException("API keys cannot be updated");
    }

    @Override
    public Optional<ApiKeyBO> delete(final String id) {
        return persistenceService.delete(id);
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
                .appId(app.getId())
                .key((String) token.getToken())
                .build();

        return create(apiKey);
    }

    @Override
    public List<ApiKeyBO> getByAppId(final String appId) {
        return apiKeysRepository.getByAppId(appId)
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()))
                .join();
    }

    @Override
    public Optional<AppBO> validateApiKey(final String key) {
        return apiKeyExchange.verifyAndGetAppId(key)
                .thenApply(optional -> optional.flatMap(applicationsService::getById))
                .join();

    }
}
