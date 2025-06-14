package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.service.model.HashedPasswordBO;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import io.smallrye.mutiny.Uni;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public abstract class AbstractSecurePassword implements SecurePassword {
    private final int saltSize;

    protected abstract Uni<byte[]> hashWithSalt(final String plain, final byte[] saltBytes);

    protected AbstractSecurePassword(final int saltSize) {
        this.saltSize = saltSize;
    }

    @Override
    public Uni<HashedPasswordBO> hash(final String plain) {
        final byte[] saltBytes = generateSalt();

        return hashWithSalt(plain, saltBytes)
                .map(hashedBytes -> HashedPasswordBO.builder()
                        .salt(Base64.getEncoder().encodeToString(saltBytes))
                        .password(Base64.getEncoder().encodeToString(hashedBytes))
                        .build());
    }

    @Override
    public Uni<Boolean> verify(final String plain, final HashedPasswordBO hashed) {
        Objects.requireNonNull(hashed.getSalt());
        Objects.requireNonNull(hashed.getPassword());

        final byte[] saltBytes = Base64.getDecoder().decode(hashed.getSalt());
        final byte[] storedHashBytes = Base64.getDecoder().decode(hashed.getPassword());

        return hashWithSalt(plain, saltBytes)
                .map(hashedBytes -> Arrays.equals(storedHashBytes, hashedBytes));
    }

    private byte[] generateSalt() {
        final CryptographicRandom random = new CryptographicRandom();

        return random.bytes(saltSize);
    }
}
