package com.nexblocks.authguard.benchmarks;

import com.nexblocks.authguard.basic.config.Pbkdf2Config;
import com.nexblocks.authguard.basic.config.Pbkdf2ConfigInterface;
import com.nexblocks.authguard.basic.passwords.Argon2Password;
import com.nexblocks.authguard.basic.passwords.BCryptPassword;
import com.nexblocks.authguard.basic.passwords.Pbkdf2Password;
import com.nexblocks.authguard.basic.passwords.SCryptPassword;
import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class PasswordHashingBenchmarks {
    @Benchmark
    @Fork(value = 1, warmups = 2)
    public void bcryptWithDefaults() {
        BCryptPassword bcrypt = new BCryptPassword();
        String password = RandomStringUtils.randomAlphanumeric(12);

        bcrypt.hash(password);
    }

    @Benchmark
    @Fork(value = 1, warmups = 2)
    public void scryptWithDefaults() {
        SCryptPassword scrypt = new SCryptPassword();
        String password = RandomStringUtils.randomAlphanumeric(12);

        scrypt.hash(password);
    }

    @Benchmark
    @Fork(value = 1, warmups = 2)
    public void argon2WithDefaults() {
        Argon2Password argon2 = new Argon2Password();
        String password = RandomStringUtils.randomAlphanumeric(12);

        argon2.hash(password);
    }

    @Benchmark
    @Fork(value = 1, warmups = 2)
    public void pbkdf2Sha256WithDefaults() {
        Pbkdf2Password pbkdf2 = new Pbkdf2Password(Pbkdf2Config.builder()
                .hashingAlgorithm(Pbkdf2ConfigInterface.Pkdf2HashingAlgorithm.SHA_256)
                .build());
        String password = RandomStringUtils.randomAlphanumeric(12);

        pbkdf2.hash(password);
    }

    @Benchmark
    @Fork(value = 1, warmups = 2)
    public void pbkdf2Sha512WithDefaults() {
        Pbkdf2Password pbkdf2 = new Pbkdf2Password(Pbkdf2Config.builder()
                .hashingAlgorithm(Pbkdf2ConfigInterface.Pkdf2HashingAlgorithm.SHA_512)
                .build());
        String password = RandomStringUtils.randomAlphanumeric(12);

        pbkdf2.hash(password);
    }
}
