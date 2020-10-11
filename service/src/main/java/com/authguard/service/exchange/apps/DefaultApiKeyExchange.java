package com.authguard.service.exchange.apps;

import com.authguard.dal.ApiKeysRepository;
import com.authguard.dal.model.ApiKeyDO;
import com.authguard.service.exchange.ApiKeyExchange;
import com.authguard.service.exchange.KeyExchange;
import com.authguard.service.keys.DefaultApiKeysProvider;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;
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
