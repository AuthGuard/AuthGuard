package com.authguard.service.random;

import java.security.SecureRandom;
import java.util.Base64;

public class CryptographicRandom {
    private final SecureRandom secureRandom;

    public CryptographicRandom() {
        secureRandom = new SecureRandom();
    }

    public String base64(final int size) {
        return Base64.getEncoder().encodeToString(bytes(size));
    }

    public byte[] bytes(final int size) {
        final byte[] bytes = new byte[size];

        secureRandom.nextBytes(bytes);

        return bytes;
    }
}
