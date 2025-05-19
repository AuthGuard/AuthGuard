package com.nexblocks.authguard.service.exchange.apps;

import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.persistence.ApiKeysRepository;
import com.nexblocks.authguard.service.config.ApiKeyHashingConfig;
import com.nexblocks.authguard.service.config.ApiKeysConfig;
import com.nexblocks.authguard.service.keys.ApiKeyHashProvider;
import com.nexblocks.authguard.service.keys.DefaultApiKeysProvider;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.EntityType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultApiKeyExchangeTest {
    private DefaultApiKeysProvider provider;
    private ApiKeysRepository repository;
    private ApiKeyHashProvider apiKeyHashProvider;

    private DefaultApiKeyExchange exchange;

    @BeforeEach
    void setup() {
        provider = new DefaultApiKeysProvider(ApiKeysConfig.builder().build());
        repository = Mockito.mock(ApiKeysRepository.class);

        apiKeyHashProvider = new ApiKeyHashProvider(ApiKeysConfig.builder()
                .hash(ApiKeyHashingConfig.builder()
                        .algorithm("blake2b")
                        .digestSize(32)
                        .key("test-key")
                        .build())
                .build());

        exchange = new DefaultApiKeyExchange(provider, repository, apiKeyHashProvider);
    }

    @Test
    void generateKey() {
        final AppBO app = AppBO.builder()
                .id(101)
                .build();

        final AuthResponseBO expected = AuthResponseBO.builder()
                .type("api_key")
                .entityType(EntityType.APPLICATION)
                .entityId(app.getId())
                .build();

        final AuthResponseBO actual = exchange.generateKey(app, null);

        assertThat(actual).isEqualToIgnoringGivenFields(expected, "token");
        assertThat(actual.getToken()).isNotNull();
    }

    @Test
    void verifyAndGetAppId() {
        final String key = "key";
        final String hashedKey = apiKeyHashProvider.getHash().hash(key);
        final long appId = 101;

        final ApiKeyDO apiKeyDO = ApiKeyDO.builder()
                .key(hashedKey)
                .appId(appId)
                .build();

        Mockito.when(repository.getByKey(hashedKey))
                .thenReturn(Uni.createFrom().item(Optional.of(apiKeyDO)));

        final Optional<Long> retrieved = exchange.verifyAndGetAppId(key).join();

        assertThat(retrieved).contains(appId);
    }

    @Test
    void verifyAndGetAppIdInvalidKey() {
        final String key = "key";
        final String hashedKey = apiKeyHashProvider.getHash().hash(key);

        Mockito.when(repository.getByKey(hashedKey))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        final Optional<Long> retrieved = exchange.verifyAndGetAppId(key).join();

        assertThat(retrieved).isEmpty();
    }

    @Test
    void verifyAndGetAppIdValidKey() {
        final String key = "key";
        final String hashedKey = apiKeyHashProvider.getHash().hash(key);
        final long appId = 101;

        final ApiKeyDO apiKeyDO = ApiKeyDO.builder()
                .key(hashedKey)
                .appId(appId)
                .expiresAt(Instant.now().plusSeconds(5))
                .build();

        Mockito.when(repository.getByKey(hashedKey))
                .thenReturn(Uni.createFrom().item(Optional.of(apiKeyDO)));

        final Optional<Long> retrieved = exchange.verifyAndGetAppId(key).join();

        assertThat(retrieved).contains(appId);
    }

    @Test
    void verifyAndGetAppIdExpiredKey() {
        final String key = "key";
        final String hashedKey = apiKeyHashProvider.getHash().hash(key);
        final long appId = 101;

        final ApiKeyDO apiKeyDO = ApiKeyDO.builder()
                .key(hashedKey)
                .appId(appId)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        Mockito.when(repository.getByKey(hashedKey))
                .thenReturn(Uni.createFrom().item(Optional.of(apiKeyDO)));

        final Optional<Long> retrieved = exchange.verifyAndGetAppId(key).join();

        assertThat(retrieved).isEmpty();
    }
}