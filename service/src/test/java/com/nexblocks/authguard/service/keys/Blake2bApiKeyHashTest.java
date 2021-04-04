package com.nexblocks.authguard.service.keys;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Blake2bApiKeyHashTest {

    final String key = "this_is_a_test_key";
    final Integer digestSize = 32;
    final Blake2bApiKeyHash blake2b = new Blake2bApiKeyHash(key, digestSize);

    @Test
    void hashAndVerify() {
        final String apiKey = "this_is_a_randomly_generated_api_key";

        final String hash = blake2b.hash(apiKey);

        assertThat(blake2b.verify(apiKey, hash)).isTrue();
    }

    @Test
    void hashAndVerifyWrongKey() {
        final String apiKey = "this_is_a_randomly_generated_api_key";

        final String hash = blake2b.hash(apiKey);

        assertThat(blake2b.verify(apiKey.substring(2), hash)).isFalse();
    }

}