package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.service.model.HashedPasswordBO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Argon2PasswordTest {
    @Test
    void hash() {
        final Argon2Password argon = new Argon2Password();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = argon.hash(password).subscribeAsCompletionStage().join();

        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword.getPassword()).isNotEqualTo(password);
        assertThat(hashedPassword.getSalt()).isNotNull();
    }

    @Test
    void verify() {
        final Argon2Password argon = new Argon2Password();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = argon.hash(password).subscribeAsCompletionStage().join();

        assertThat(argon.verify(password, hashedPassword).subscribeAsCompletionStage().join())
                .isTrue();
    }

    @Test
    void verifyMismatch() {
        final Argon2Password argon = new Argon2Password();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = argon.hash(password).subscribeAsCompletionStage().join();

        assertThat(argon.verify("should not match", hashedPassword).subscribeAsCompletionStage().join())
                .isFalse();
    }
}
