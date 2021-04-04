package com.nexblocks.authguard.service.keys;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.ApiKeyHashingConfig;
import com.nexblocks.authguard.service.config.ApiKeysConfig;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;

public class ApiKeyHashProvider {
    private final ApiKeysConfig apiKeysConfig;

    @Inject
    public ApiKeyHashProvider(final @Named("apiKeys") ConfigContext config) {
        this(config.asConfigBean(ApiKeysConfig.class));
    }

    public ApiKeyHashProvider(final ApiKeysConfig apiKeysConfig) {
        this.apiKeysConfig = apiKeysConfig;
    }

    public ApiKeyHash getHash() {
        final ApiKeyHashingConfig hashingConfig = apiKeysConfig.getHash();

        if (hashingConfig == null) {
            throw new ConfigurationException("Missing configuration property 'hash' in API key configuration");
        }

        if ("blake2b".equals(hashingConfig.getAlgorithm())) {
            if (hashingConfig.getKey() == null) {
                throw new ConfigurationException("A hashing key must be provided for blake2b");
            }

            return new Blake2bApiKeyHash(hashingConfig.getKey(), hashingConfig.getDigestSize());
        }

        throw new ConfigurationException("Unknown API hashing algorithm");
    }
}
