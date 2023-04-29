package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.persistence.ApiKeysRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.exchange.KeyExchange;
import com.nexblocks.authguard.service.keys.ApiKeyHash;
import com.nexblocks.authguard.service.keys.ApiKeyHashProvider;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApiKeysServiceImpl implements ApiKeysService {
    private static final Logger LOG = LoggerFactory.getLogger(ApiKeysServiceImpl.class);

    private static final String API_KEYS_CHANNELS = "api_keys";

    private final ApplicationsService applicationsService;
    private final Map<String, ApiKeyExchange> apiKeyExchangesByType;
    private final ApiKeysRepository apiKeysRepository;
    private final ApiKeyHash apiKeyHash;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<ApiKeyBO, ApiKeyDO, ApiKeysRepository> persistenceService;

    @Inject
    public ApiKeysServiceImpl(final ApplicationsService applicationsService,
                              final List<ApiKeyExchange> apiKeyExchanges,
                              final ApiKeysRepository apiKeysRepository,
                              final ApiKeyHashProvider apiKeyHashProvider,
                              final MessageBus messageBus,
                              final ServiceMapper serviceMapper) {
        this.applicationsService = applicationsService;
        this.apiKeysRepository = apiKeysRepository;
        this.apiKeyHash = apiKeyHashProvider.getHash();
        this.serviceMapper = serviceMapper;

        this.apiKeyExchangesByType = mapExchanges(apiKeyExchanges);

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
        LOG.info("API key delete request. accountId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public ApiKeyBO generateApiKey(final String appId, final String type, final Duration duration) {
        final AppBO app = applicationsService.getById(appId)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST,
                        "No app with ID " + appId + " found"));

        return generateApiKey(app, type, duration);
    }

    @Override
    public ApiKeyBO generateApiKey(final AppBO app, final String type, final Duration duration) {
        final ApiKeyExchange apiKeyExchange = getExchangeOrFail(type);

        LOG.info("API key request. appId={}, domain={}, type={}, duration={}",
                app.getId(), app.getDomain(), type, duration);

        final Instant expirationInstant = getExpirationInstant(duration);
        final AuthResponseBO token = apiKeyExchange.generateKey(app, expirationInstant);
        final String generatedKey = (String) token.getToken();
        final String hashedKey = apiKeyHash.hash(generatedKey);
        final ApiKeyBO toCreate = mapApiKey(app.getId(), hashedKey, type, expirationInstant);

        final ApiKeyBO persisted = create(toCreate);

        LOG.info("API key generated. appId={}, domain={}, type={}, keyId={}, expiresAt={}",
                app.getId(), app.getDomain(), type, persisted.getId(), persisted.getExpiresAt());

        return persisted.withKey(generatedKey); // we store the hashed version, but we return the clear version
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
    public Optional<AppBO> validateApiKey(final String key, final String type) {
        final ApiKeyExchange apiKeyExchange = getExchangeOrFail(type);

        return apiKeyExchange.verifyAndGetAppId(key)
                .thenApply(optional -> optional.flatMap(applicationsService::getById))
                .join();

    }

    private ApiKeyBO mapApiKey(final String appId, final String key, final String type, final Instant expiresAt) {
        final ApiKeyBO.Builder builder = ApiKeyBO.builder()
                .appId(appId)
                .key(key)
                .type(type);

        if (expiresAt != null) {
            builder.expiresAt(expiresAt);
        }

        return builder.build();
    }

    private Instant getExpirationInstant(final Duration duration) {
        if (duration != null && !duration.isZero()) {
            return Instant.now().plus(duration);
        }

        return null;
    }

    private ApiKeyExchange getExchangeOrFail(final String type) {
        return Optional.ofNullable(apiKeyExchangesByType.get(type))
                .orElseThrow(() -> new ServiceException(
                        ErrorCode.INVALID_API_KEY_TYPE,
                        "API key type " + type + " does not exist"));
    }

    private Map<String, ApiKeyExchange> mapExchanges(final List<ApiKeyExchange> exchanges) {
        return exchanges.stream()
                .filter(exchange -> exchange.getClass().getAnnotation(KeyExchange.class) != null)
                .collect(Collectors.toMap(
                        this::keyExchangeToString,
                        Function.identity()
                ));
    }

    private String keyExchangeToString(final ApiKeyExchange exchange) {
        final KeyExchange exchangeAnnotation = exchange.getClass().getAnnotation(KeyExchange.class);

        return exchangeAnnotation.keyType();
    }
}
