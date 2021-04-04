package com.nexblocks.authguard.service.exchange.apps;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.persistence.ApiKeysRepository;
import com.nexblocks.authguard.service.exchange.ApiKeyExchange;
import com.nexblocks.authguard.service.exchange.KeyExchange;
import com.nexblocks.authguard.service.keys.ApiKeyHash;
import com.nexblocks.authguard.service.keys.ApiKeyHashProvider;
import com.nexblocks.authguard.service.keys.DefaultApiKeysProvider;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.TokensBO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@KeyExchange(keyType = "default")
public class DefaultApiKeyExchange implements ApiKeyExchange {
    private static final String TOKEN_TYPE = "api_key";

    private final DefaultApiKeysProvider provider;
    private final ApiKeyHash apiKeyHash;
    private final ApiKeysRepository repository;

    @Inject
    public DefaultApiKeyExchange(final DefaultApiKeysProvider provider, final ApiKeysRepository repository,
                                 final ApiKeyHashProvider hashProvider) {
        this.provider = provider;
        this.repository = repository;
        this.apiKeyHash = hashProvider.getHash();;
    }

    @Override
    public TokensBO generateKey(final AppBO app) {
        return TokensBO.builder()
                .entityType(EntityType.APPLICATION)
                .entityId(app.getId())
                .type(TOKEN_TYPE)
                .token(provider.generateKey())
                .build();
    }

    @Override
    public CompletableFuture<Optional<String>> verifyAndGetAppId(final String apiKey) {
        return repository.getByKey(apiKeyHash.hash(apiKey))
                .thenApply(optional -> optional.map(ApiKeyDO::getAppId));
    }
}
