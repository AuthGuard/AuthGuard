package com.nexblocks.authguard.service.keys;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.ApiKeysConfig;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;

@ProvidesToken("apiKey")
public class DefaultApiKeysProvider implements AuthProvider {
    private final String TOKEN_TYPE = "api_key";

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

    @Override
    public AuthResponseBO generateToken(final AccountBO account) {
        throw new UnsupportedOperationException("API keys cannot be generated for an account");
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        final String token = cryptographicRandom.base64Url(config.getRandomSize());

        return AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(token)
                .entityType(EntityType.APPLICATION)
                .entityId(app.getId())
                .build();
    }

    @Override
    public AuthResponseBO generateToken(ClientBO client) {
        final String token = cryptographicRandom.base64Url(config.getRandomSize());

        return AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(token)
                .entityType(EntityType.CLIENT)
                .entityId(client.getId())
                .build();
    }
}
