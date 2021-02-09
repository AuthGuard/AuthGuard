package com.nexblocks.authguard.service.exchange.apps;

import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.persistence.ApiKeysRepository;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.exchange.KeyExchange;
import com.nexblocks.authguard.service.keys.DefaultApiKeysProvider;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.TokensBO;
import com.google.inject.Inject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@KeyExchange(keyType = "default")
public class DefaultApiKeyExchange implements ApiKeyExchange {
    private final DefaultApiKeysProvider provider;
    private final ApiKeysRepository repository;

    @Inject
    public DefaultApiKeyExchange(final DefaultApiKeysProvider provider, final ApiKeysRepository repository) {
        this.provider = provider;
        this.repository = repository;
    }

    @Override
    public TokensBO generateKey(final AppBO app) {
        return TokensBO.builder()
                .token(provider.generateKey())
                .type("API key")
                .build();
    }

    @Override
    public CompletableFuture<Optional<String>> verifyAndGetAppId(final String apiKey) {
        return repository.getByKey(apiKey)
                .thenApply(optional -> optional.map(ApiKeyDO::getAppId));
    }
}
