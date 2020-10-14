package com.authguard.service.keys;

import com.authguard.service.config.ApiKeysConfig;
import com.authguard.service.keys.DefaultApiKeysProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultApiKeysProviderTest {

    @Test
    void generateKey() {
        final DefaultApiKeysProvider provider = new DefaultApiKeysProvider(ApiKeysConfig.builder().build());

        final String key = provider.generateKey();

        assertThat(key).hasSizeGreaterThan(10);
    }

}