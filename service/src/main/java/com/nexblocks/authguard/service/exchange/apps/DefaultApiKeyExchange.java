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
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.ClientBO;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@KeyExchange(keyType = "default")
public class DefaultApiKeyExchange implements ApiKeyExchange {
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
    public AuthResponseBO generateKey(final AppBO app, Instant expiresAt) {
        return provider.generateToken(app); // expiry time isn't reflected in the key itself, so we don't care about it here
    }

    @Override
    public AuthResponseBO generateKey(ClientBO client, Instant expiresAt) {
        return provider.generateToken(client);
    }

    @Override
    public CompletableFuture<Optional<Long>> verifyAndGetAppId(final String apiKey) {
        return repository.getByKey(apiKeyHash.hash(apiKey)).subscribe().asCompletionStage()
                .thenApply(optional -> optional
                        .filter(this::isValid)
                        .map(ApiKeyDO::getAppId));
    }

    @Override
    public CompletableFuture<Optional<Long>> verifyAndGetClientId(String apiKey) {
        return verifyAndGetAppId(apiKey);
    }

    private boolean isValid(final ApiKeyDO apiKeyDO) {
        if (apiKeyDO.isDeleted()) {
            return false;
        }

        if (apiKeyDO.getExpiresAt() == null) {
            return true;
        }

        return apiKeyDO.getExpiresAt().isAfter(Instant.now());
    }
}
