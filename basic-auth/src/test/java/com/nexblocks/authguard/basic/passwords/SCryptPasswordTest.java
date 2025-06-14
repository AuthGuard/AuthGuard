package com.nexblocks.authguard.basic.passwords;

import org.apache.commons.lang3.RandomStringUtils;
import com.nexblocks.authguard.service.model.HashedPasswordBO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SCryptPasswordTest {
    @Test
    void hash() {
        final SCryptPassword scrypt = new SCryptPassword();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = scrypt.hash(password).subscribeAsCompletionStage().join();

        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword.getPassword()).isNotEqualTo(password);
        assertThat(hashedPassword.getSalt()).isNotNull();
    }

    @Test
    void verify() {
        final SCryptPassword scrypt = new SCryptPassword();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = scrypt.hash(password).subscribeAsCompletionStage().join();

        assertThat(scrypt.verify(password, hashedPassword).subscribeAsCompletionStage().join())
                .isTrue();
    }

    @Test
    void verifyMismatch() {
        final SCryptPassword scrypt = new SCryptPassword();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = scrypt.hash(password).subscribeAsCompletionStage().join();

        assertThat(scrypt.verify("should not match", hashedPassword).subscribeAsCompletionStage().join())
                .isFalse();
    }
}