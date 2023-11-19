package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.basic.config.Pbkdf2Config;
import com.nexblocks.authguard.basic.config.Pbkdf2ConfigInterface;
import com.nexblocks.authguard.service.model.HashedPasswordBO;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

class Pbkdf2PasswordTest {
    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void hashSha256() {
        final Pbkdf2Password pbkdf = new Pbkdf2Password(Pbkdf2Config.builder()
                .hashingAlgorithm(Pbkdf2ConfigInterface.Pkdf2HashingAlgorithm.SHA_256)
                .build());
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = pbkdf.hash(password);

        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword.getPassword()).isNotEqualTo(password);
        assertThat(hashedPassword.getSalt()).isNotNull();
    }

    @Test
    void verifySha256() {
        final Pbkdf2Password pbkdf = new Pbkdf2Password(Pbkdf2Config.builder()
                .hashingAlgorithm(Pbkdf2ConfigInterface.Pkdf2HashingAlgorithm.SHA_256)
                .build());
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = pbkdf.hash(password);

        assertThat(pbkdf.verify(password, hashedPassword)).isTrue();
    }

    @Test
    void verifyMismatch256() {
        final Pbkdf2Password pbkdf = new Pbkdf2Password(Pbkdf2Config.builder()
                .hashingAlgorithm(Pbkdf2ConfigInterface.Pkdf2HashingAlgorithm.SHA_256)
                .build());
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = pbkdf.hash(password);

        assertThat(pbkdf.verify("should not match", hashedPassword)).isFalse();
    }

    @Test
    void hashSha512() {
        final Pbkdf2Password pbkdf = new Pbkdf2Password(Pbkdf2Config.builder()
                .hashingAlgorithm(Pbkdf2ConfigInterface.Pkdf2HashingAlgorithm.SHA_512)
                .build());
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = pbkdf.hash(password);

        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword.getPassword()).isNotEqualTo(password);
        assertThat(hashedPassword.getSalt()).isNotNull();
    }

    @Test
    void verifySha512() {
        final Pbkdf2Password pbkdf = new Pbkdf2Password(Pbkdf2Config.builder()
                .hashingAlgorithm(Pbkdf2ConfigInterface.Pkdf2HashingAlgorithm.SHA_512)
                .build());
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = pbkdf.hash(password);

        assertThat(pbkdf.verify(password, hashedPassword)).isTrue();
    }

    @Test
    void verifyMismatch512() {
        final Pbkdf2Password pbkdf = new Pbkdf2Password(Pbkdf2Config.builder()
                .hashingAlgorithm(Pbkdf2ConfigInterface.Pkdf2HashingAlgorithm.SHA_512)
                .build());
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final HashedPasswordBO hashedPassword = pbkdf.hash(password);

        assertThat(pbkdf.verify("should not match", hashedPassword)).isFalse();
    }
}