package com.nexblocks.authguard.basic.passwords;

import org.apache.commons.lang3.RandomStringUtils;
import com.nexblocks.authguard.service.model.HashedPasswordBO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordTest {
    @Test
    void hash() {
        final BCryptPassword bcrypt = new BCryptPassword();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = bcrypt.hash(password);

        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword.getPassword()).isNotEqualTo(password);
        assertThat(hashedPassword.getSalt()).isNotNull();
    }

    @Test
    void verify() {
        final BCryptPassword scrypt = new BCryptPassword();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = scrypt.hash(password);

        assertThat(scrypt.verify(password, hashedPassword)).isTrue();
    }

    @Test
    void verifyMismatch() {
        final BCryptPassword scrypt = new BCryptPassword();
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = scrypt.hash(password);

        assertThat(scrypt.verify("should not match", hashedPassword)).isFalse();
    }
}
