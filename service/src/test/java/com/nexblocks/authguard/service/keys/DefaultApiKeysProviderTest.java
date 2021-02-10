package com.nexblocks.authguard.service.keys;

import com.nexblocks.authguard.service.config.ApiKeysConfig;
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