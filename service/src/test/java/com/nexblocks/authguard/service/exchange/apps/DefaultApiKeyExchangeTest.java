package com.nexblocks.authguard.service.exchange.apps;

import com.nexblocks.authguard.dal.persistence.ApiKeysRepository;
import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.service.config.ApiKeysConfig;
import com.nexblocks.authguard.service.keys.DefaultApiKeysProvider;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.TokensBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultApiKeyExchangeTest {
    private DefaultApiKeysProvider provider;
    private ApiKeysRepository repository;

    private DefaultApiKeyExchange exchange;

    @BeforeEach
    void setup() {
        provider = new DefaultApiKeysProvider(ApiKeysConfig.builder().build());
        repository = Mockito.mock(ApiKeysRepository.class);

        exchange = new DefaultApiKeyExchange(provider, repository);
    }

    @Test
    void generateKey() {
        final TokensBO token = exchange.generateKey(AppBO.builder().build());

        assertThat(token.getType()).isEqualTo("API key");
        assertThat(token.getToken()).isNotNull();
    }

    @Test
    void verifyAndGetAppId() {
        final String key = "key";
        final String appId = "app";

        final ApiKeyDO apiKeyDO = ApiKeyDO.builder()
                .key(key)
                .appId(appId)
                .build();

        Mockito.when(repository.getByKey(key))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(apiKeyDO)));

        final Optional<String> retrieved = exchange.verifyAndGetAppId(key).join();

        assertThat(retrieved).contains(appId);
    }
}