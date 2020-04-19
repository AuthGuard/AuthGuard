package com.authguard.service.passwords;

import com.authguard.service.model.HashedPasswordBO;
import com.authguard.service.random.CryptographicRandom;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public abstract class AbstractSecurePassword implements SecurePassword {
    private final int saltSize;

    protected abstract byte[] hashWithSalt(final String plain, final byte[] saltBytes);

    protected AbstractSecurePassword(final int saltSize) {
        this.saltSize = saltSize;
    }

    @Override
    public HashedPasswordBO hash(final String plain) {
        final byte[] saltBytes = generateSalt();
        final byte[] hashedBytes = hashWithSalt(plain, saltBytes);

        return HashedPasswordBO.builder()
                .salt(Base64.getEncoder().encodeToString(saltBytes))
                .password(Base64.getEncoder().encodeToString(hashedBytes))
                .build();
    }

    @Override
    public boolean verify(final String plain, final HashedPasswordBO hashed) {
        Objects.requireNonNull(hashed.getSalt());
        Objects.requireNonNull(hashed.getPassword());

        final byte[] saltBytes = Base64.getDecoder().decode(hashed.getSalt());
        final byte[] storedHashBytes = Base64.getDecoder().decode(hashed.getPassword());

        final byte[] hashBytes = hashWithSalt(plain, saltBytes);

        return Arrays.equals(storedHashBytes, hashBytes);
    }

    private byte[] generateSalt() {
        final CryptographicRandom random = new CryptographicRandom();

        return random.bytes(saltSize);
    }
}
