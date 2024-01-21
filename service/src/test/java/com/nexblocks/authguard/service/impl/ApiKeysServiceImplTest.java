package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.persistence.ApiKeysRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.config.ApiKeyHashingConfig;
import com.nexblocks.authguard.service.config.ApiKeysConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.exchange.KeyExchange;
import com.nexblocks.authguard.service.keys.ApiKeyHash;
import com.nexblocks.authguard.service.keys.ApiKeyHashProvider;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.ApiKeyBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.ClientBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeysServiceImplTest {

    private ApplicationsService applicationsService;
    private ClientsService clientsService;
    private ApiKeysRepository apiKeysRepository;
    private ApiKeyExchange apiKeyExchange;
    private ApiKeyHash apiKeyHash;
    private ServiceMapper serviceMapper;
    private ApiKeysService apiKeysService;

    @KeyExchange(keyType = "test")
    private static class TestApiKeyExchange implements ApiKeyExchange {

        @Override
        public AuthResponseBO generateKey(AppBO app, Instant expiresAt) {
            return AuthResponseBO.builder()
                    .token("key")
                    .build();
        }

        @Override
        public AuthResponseBO generateKey(ClientBO client, Instant expiresAt) {
            return AuthResponseBO.builder()
                    .token("key")
                    .build();
        }

        @Override
        public CompletableFuture<Optional<Long>> verifyAndGetAppId(String apiKey) {
            return CompletableFuture.completedFuture(Optional.of(1L));
        }

        @Override
        public CompletableFuture<Optional<Long>> verifyAndGetClientId(String apiKey) {
            return CompletableFuture.completedFuture(Optional.of(2L));
        }
    }

    @BeforeEach
    void setup() {
        applicationsService = Mockito.mock(ApplicationsService.class);
        clientsService = Mockito.mock(ClientsService.class);
        apiKeysRepository = Mockito.mock(ApiKeysRepository.class);
        apiKeyExchange = Mockito.mock(ApiKeyExchange.class);
        MessageBus messageBus = Mockito.mock(MessageBus.class);

        ApiKeyHashProvider apiKeyHashProvider = new ApiKeyHashProvider(ApiKeysConfig.builder()
                .hash(ApiKeyHashingConfig.builder()
                        .algorithm("blake2b")
                        .digestSize(32)
                        .key("test-key")
                        .build())
                .build());

        apiKeyHash = apiKeyHashProvider.getHash();

        serviceMapper = new ServiceMapperImpl();

        apiKeysService = new ApiKeysServiceImpl(applicationsService,
                clientsService, Collections.singletonList(new TestApiKeyExchange()),
                apiKeysRepository, apiKeyHashProvider, messageBus, serviceMapper);
    }

    @Test
    void generateApiKeyWithoutExpiration() {
        long appId = 1;
        String key = "key";
        AppBO app = AppBO.builder()
                .id(appId)
                .build();

        Mockito.when(applicationsService.getById(appId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        Mockito.when(apiKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, ApiKeyDO.class)));

        Mockito.when(apiKeyExchange.generateKey(app, null))
                .thenReturn(AuthResponseBO.builder()
                        .token(key)
                        .build());

        ApiKeyBO actual = apiKeysService.generateApiKey(appId, "main", "test", Duration.ZERO).join();

        assertThat(actual.getAppId()).isEqualTo(appId);
        assertThat(actual.getKey()).isEqualTo(key);
        assertThat(actual.isForClient()).isFalse();

        // verify that we persisted the hashed key and not the clear key

        ArgumentCaptor<ApiKeyDO> argumentCaptor = ArgumentCaptor.forClass(ApiKeyDO.class);

        Mockito.verify(apiKeysRepository).save(argumentCaptor.capture());

        ApiKeyDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAppId()).isEqualTo(appId);
        assertThat(persisted.getKey()).isEqualTo(apiKeyHash.hash(key));
        assertThat(persisted.getType()).isEqualTo("test");
        assertThat(persisted.isForClient()).isFalse();
        assertThat(persisted.getExpiresAt()).isNull();
    }

    @Test
    void generateApiKeyWithExpiration() {
        long appId = 1;
        String key = "key";
        AppBO app = AppBO.builder()
                .id(appId)
                .build();
        Duration duration = Duration.ofDays(1);

        Mockito.when(applicationsService.getById(appId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        Mockito.when(apiKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, ApiKeyDO.class)));

        Mockito.when(apiKeyExchange.generateKey(app, null))
                .thenReturn(AuthResponseBO.builder()
                        .token(key)
                        .build());

        ApiKeyBO actual = apiKeysService.generateApiKey(appId, "main", "test", duration).join();

        assertThat(actual.getAppId()).isEqualTo(appId);
        assertThat(actual.getKey()).isEqualTo(key);
        assertThat(actual.isForClient()).isFalse();

        // verify that we persisted the hashed key and not the clear key

        ArgumentCaptor<ApiKeyDO> argumentCaptor = ArgumentCaptor.forClass(ApiKeyDO.class);

        Mockito.verify(apiKeysRepository).save(argumentCaptor.capture());

        ApiKeyDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAppId()).isEqualTo(appId);
        assertThat(persisted.getKey()).isEqualTo(apiKeyHash.hash(key));
        assertThat(persisted.getType()).isEqualTo("test");
        assertThat(persisted.isForClient()).isFalse();
        assertThat(persisted.getExpiresAt())
                .isBetween(Instant.now().plus(Duration.ofDays(1)).minusSeconds(1),
                        Instant.now().plus(Duration.ofDays(1)).plusSeconds(1));
    }

    @Test
    void generateApiKeyNonExistingApp() {
        long appId = 1;

        Mockito.when(applicationsService.getById(appId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> apiKeysService.generateApiKey(appId, "main", "test", Duration.ZERO).join())
                .hasCauseInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void generateApiKeyInvalidType() {
        long appId = 1;
        AppBO app = AppBO.builder()
                .id(appId)
                .build();

        Mockito.when(applicationsService.getById(appId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        assertThatThrownBy(() -> apiKeysService.generateApiKey(appId, "main", "none", Duration.ZERO).join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void getByAppId() {
        long appId = 1;
        String key = "key";
        ApiKeyBO apiKeyBO = ApiKeyBO.builder()
                .appId(appId)
                .key(key)
                .build();
        ApiKeyDO apiKeyDO = serviceMapper.toDO(apiKeyBO);

        Mockito.when(apiKeysRepository.getByAppId(appId))
                .thenReturn(CompletableFuture.completedFuture(Collections.singletonList(apiKeyDO)));

        List<ApiKeyBO> actual = apiKeysService.getByAppId(appId, "main").join();

        assertThat(actual).isEqualTo(Collections.singletonList(apiKeyBO));
    }

    @Test
    void validateApiKey() {
        long appId = 1;
        String key = "key";
        AppBO app = AppBO.builder()
                .id(appId)
                .build();

        Mockito.when(apiKeyExchange.verifyAndGetAppId(key))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(appId)));

        Mockito.when(applicationsService.getById(appId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        AppBO actual = apiKeysService.validateApiKey(key, "main", "test").join();

        assertThat(actual).isEqualTo(app);
    }

    @Test
    void generateClientApiKeyWithoutExpiration() {
        long clientId = 2;
        String key = "key";
        ClientBO app = ClientBO.builder()
                .id(clientId)
                .build();

        Mockito.when(clientsService.getById(clientId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(app)));

        Mockito.when(apiKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, ApiKeyDO.class)));

        Mockito.when(apiKeyExchange.generateKey(app, null))
                .thenReturn(AuthResponseBO.builder()
                        .token(key)
                        .build());

        ApiKeyBO actual = apiKeysService.generateClientApiKey(clientId, "main", "test", Duration.ZERO).join();

        assertThat(actual.getAppId()).isEqualTo(clientId);
        assertThat(actual.getKey()).isEqualTo(key);
        assertThat(actual.isForClient()).isTrue();

        // verify that we persisted the hashed key and not the clear key

        ArgumentCaptor<ApiKeyDO> argumentCaptor = ArgumentCaptor.forClass(ApiKeyDO.class);

        Mockito.verify(apiKeysRepository).save(argumentCaptor.capture());

        ApiKeyDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAppId()).isEqualTo(clientId);
        assertThat(persisted.getKey()).isEqualTo(apiKeyHash.hash(key));
        assertThat(persisted.getType()).isEqualTo("test");
        assertThat(persisted.isForClient()).isTrue();
        assertThat(persisted.getExpiresAt()).isNull();
    }

    @Test
    void generateClientApiKeyWithExpiration() {
        long clientId = 2;
        String key = "key";
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .build();
        Duration duration = Duration.ofDays(1);

        Mockito.when(clientsService.getById(clientId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(client)));

        Mockito.when(apiKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, ApiKeyDO.class)));

        Mockito.when(apiKeyExchange.generateKey(client, null))
                .thenReturn(AuthResponseBO.builder()
                        .token(key)
                        .build());

        ApiKeyBO actual = apiKeysService.generateClientApiKey(clientId, "main", "test", duration).join();

        assertThat(actual.getAppId()).isEqualTo(clientId);
        assertThat(actual.getKey()).isEqualTo(key);
        assertThat(actual.isForClient()).isTrue();

        // verify that we persisted the hashed key and not the clear key

        ArgumentCaptor<ApiKeyDO> argumentCaptor = ArgumentCaptor.forClass(ApiKeyDO.class);

        Mockito.verify(apiKeysRepository).save(argumentCaptor.capture());

        ApiKeyDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAppId()).isEqualTo(clientId);
        assertThat(persisted.getKey()).isEqualTo(apiKeyHash.hash(key));
        assertThat(persisted.getType()).isEqualTo("test");
        assertThat(persisted.isForClient()).isTrue();
        assertThat(persisted.getExpiresAt())
                .isBetween(Instant.now().plus(Duration.ofDays(1)).minusSeconds(1),
                        Instant.now().plus(Duration.ofDays(1)).plusSeconds(1));
    }

    @Test
    void generateClientApiKeyNonExistingApp() {
        long clientId = 2;

        Mockito.when(clientsService.getById(clientId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> apiKeysService.generateClientApiKey(clientId, "main", "test", Duration.ZERO).join())
                .hasCauseInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void generateClientApiKeyInvalidType() {
        long clientId = 1;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .build();

        Mockito.when(clientsService.getById(clientId, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(client)));

        assertThatThrownBy(() -> apiKeysService.generateClientApiKey(clientId, "main", "none", Duration.ZERO).join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void validateClientApiKey() {
        long clientId = 2;
        String key = "key";
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .build();

        Mockito.when(apiKeyExchange.verifyAndGetAppId(key))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(clientId)));

        Mockito.when(clientsService.getByIdUnchecked(clientId))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(client)));

        ClientBO actual = apiKeysService.validateClientApiKey(key, "test").join();

        assertThat(actual).isEqualTo(client);
    }

    @Test
    void getById() {
        long id = 3;
        ApiKeyBO apiKeyBO = ApiKeyBO.builder()
                .id(id)
                .build();
        ApiKeyDO apiKeyDO = ApiKeyDO.builder()
                .id(id)
                .build();

        Mockito.when(apiKeysRepository.getById(id))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(apiKeyDO)));

        Optional<ApiKeyBO> actual = apiKeysService.getById(id, "main").join();

        assertThat(actual).contains(apiKeyBO);
    }

    @Test
    void delete() {
        long id = 3;
        ApiKeyBO apiKeyBO = ApiKeyBO.builder()
                .id(id)
                .build();
        ApiKeyDO apiKeyDO = ApiKeyDO.builder()
                .id(id)
                .build();

        Mockito.when(apiKeysRepository.getById(id))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(apiKeyDO)));

        Mockito.when(apiKeysRepository.delete(id))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(apiKeyDO)));

        Optional<ApiKeyBO> actual = apiKeysService.delete(id, "main").join();

        assertThat(actual).contains(apiKeyBO);
    }
}