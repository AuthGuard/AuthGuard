package com.nexblocks.authguard.service.keys;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.ApiKeysConfig;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DefaultApiKeysProvider {
    private final CryptographicRandom cryptographicRandom;
    private final ApiKeysConfig config;

    @Inject
    public DefaultApiKeysProvider(final @Named("apiKeys") ConfigContext config) {
        this(config.asConfigBean(ApiKeysConfig.class));
    }

    public DefaultApiKeysProvider(final ApiKeysConfig config) {
        this.cryptographicRandom = new CryptographicRandom();
        this.config = config;
    }

    public String generateKey() {
        return cryptographicRandom.base64Url(config.getRandomSize());
    }
}
