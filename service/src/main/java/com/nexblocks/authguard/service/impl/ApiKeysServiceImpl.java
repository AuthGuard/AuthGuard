package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.persistence.ApiKeysRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.exchange.KeyExchange;
import com.nexblocks.authguard.service.keys.ApiKeyHash;
import com.nexblocks.authguard.service.keys.ApiKeyHashProvider;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.ClientBO;
import com.nexblocks.authguard.service.util.AsyncUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.smallrye.mutiny.Uni;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApiKeysServiceImpl implements ApiKeysService {
    private static final Logger LOG = LoggerFactory.getLogger(ApiKeysServiceImpl.class);

    private static final String API_KEYS_CHANNELS = "api_keys";

    private final ApplicationsService applicationsService;
    private final ClientsService clientsService;
    private final Map<String, ApiKeyExchange> apiKeyExchangesByType;
    private final ApiKeysRepository apiKeysRepository;
    private final ApiKeyHash apiKeyHash;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<ApiKeyBO, ApiKeyDO, ApiKeysRepository> persistenceService;

    @Inject
    public ApiKeysServiceImpl(final ApplicationsService applicationsService,
                              final ClientsService clientsService,
                              final List<ApiKeyExchange> apiKeyExchanges,
                              final ApiKeysRepository apiKeysRepository,
                              final ApiKeyHashProvider apiKeyHashProvider,
                              final MessageBus messageBus,
                              final ServiceMapper serviceMapper) {
        this.applicationsService = applicationsService;
        this.clientsService = clientsService;
        this.apiKeysRepository = apiKeysRepository;
        this.apiKeyHash = apiKeyHashProvider.getHash();
        this.serviceMapper = serviceMapper;

        this.apiKeyExchangesByType = mapExchanges(apiKeyExchanges);

        this.persistenceService = new PersistenceService<>(apiKeysRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, API_KEYS_CHANNELS);
    }

    @Override
    public Uni<ApiKeyBO> create(final ApiKeyBO apiKey) {
        return persistenceService.create(apiKey);
    }

    @Override
    public Uni<Optional<ApiKeyBO>> getById(final long apiKeyId, final String domain) {
        return persistenceService.getById(apiKeyId);
    }

    @Override
    public Uni<Optional<ApiKeyBO>> update(final ApiKeyBO entity, final String domain) {
        throw new UnsupportedOperationException("API keys cannot be updated");
    }

    @Override
    public Uni<Optional<ApiKeyBO>> delete(final long id, final String domain) {
        LOG.info("API key delete request. accountId={}", id);

        return getById(id, domain).flatMap(ignored -> persistenceService.delete(id));
    }

    @Override
    public Uni<ApiKeyBO> generateApiKey(final long appId, final String domain, final String type,
                                                      final String name, final Duration duration) {
        return applicationsService.getById(appId, domain)
                .flatMap(AsyncUtils::uniFromAppOptional)
                .flatMap(app -> generateApiKey(app, type, name, duration));
    }

    @Override
    public Uni<ApiKeyBO> generateClientApiKey(final long clientId, final String domain, final String type,
                                                            final String name, final Duration duration) {
        return clientsService.getById(clientId, domain)
                .flatMap(AsyncUtils::uniFromClientOptional)
                .flatMap(client -> generateClientApiKey(client, type, name, duration));
    }

    @Override
    public Uni<ApiKeyBO> generateApiKey(final AppBO app, final String type,
                                                      final String name, final Duration duration) {
        ApiKeyExchange apiKeyExchange = getExchangeOrFail(type);

        LOG.info("API key request. appId={}, domain={}, type={}, duration={}",
                app.getId(), app.getDomain(), type, duration);

        Instant expirationInstant = getExpirationInstant(duration);
        AuthResponseBO token = apiKeyExchange.generateKey(app, expirationInstant);
        String generatedKey = (String) token.getToken();
        String hashedKey = apiKeyHash.hash(generatedKey);
        ApiKeyBO toCreate = mapApiKey(app.getId(), hashedKey, type, false, name, expirationInstant);

        return create(toCreate)
                .map(persisted -> {
                    LOG.info("API key generated. appId={}, domain={}, type={}, keyId={}, expiresAt={}",
                            app.getId(), app.getDomain(), type, persisted.getId(), persisted.getExpiresAt());

                    return persisted.withKey(generatedKey); // we store the hashed version, but we return the clear version
                });
    }

    @Override
    public Uni<ApiKeyBO> generateClientApiKey(final ClientBO client, final String type,
                                                            final String name, final Duration duration) {
        ApiKeyExchange apiKeyExchange = getExchangeOrFail(type);

        LOG.info("API key request. clientId={}, domain={}, type={}, duration={}",
                client.getId(), client.getDomain(), type, duration);

        Instant expirationInstant = getExpirationInstant(duration);
        AuthResponseBO token = apiKeyExchange.generateKey(client, expirationInstant);
        String generatedKey = (String) token.getToken();
        String hashedKey = apiKeyHash.hash(generatedKey);
        ApiKeyBO toCreate = mapApiKey(client.getId(), hashedKey, type, true, name, expirationInstant);

        return create(toCreate)
                .map(persisted -> {
                    LOG.info("API key generated. clientId={}, domain={}, type={}, keyId={}, expiresAt={}",
                            client.getId(), client.getDomain(), type, persisted.getId(), persisted.getExpiresAt());

                    return persisted.withKey(generatedKey); // we store the hashed version, but we return the clear version
                });
    }

    @Override
    public Uni<List<ApiKeyBO>> getByAppId(final long appId, final String domain) {
        return apiKeysRepository.getByAppId(appId)
                .map(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<AppBO> validateApiKey(final String key, final String domain, final String type) {
        ApiKeyExchange apiKeyExchange = getExchangeOrFail(type);

        return apiKeyExchange.verifyAndGetAppId(key)
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.INVALID_TOKEN, "Token is invalid or expired"));
                    }

                    return applicationsService.getById(optional.get(), domain);
                })
                .flatMap(AsyncUtils::uniFromAppOptional);
    }

    @Override
    public Uni<ClientBO> validateClientApiKey(final String key, final String type) {
        ApiKeyExchange apiKeyExchange = getExchangeOrFail(type);

        return apiKeyExchange.verifyAndGetClientId(key)
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.INVALID_TOKEN, "Token is invalid or expired"));
                    }

                    return clientsService.getByIdUnchecked(optional.get());
                })
                .flatMap(AsyncUtils::uniFromClientOptional);
    }

    private ApiKeyBO mapApiKey(final long appId, final String key, final String type, boolean forClient,
                               final String name, final Instant expiresAt) {
        ApiKeyBO.Builder builder = ApiKeyBO.builder()
                .appId(appId)
                .key(key)
                .type(type)
                .name(name)
                .forClient(forClient);

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
