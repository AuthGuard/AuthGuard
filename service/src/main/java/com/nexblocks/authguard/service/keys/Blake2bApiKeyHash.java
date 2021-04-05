package com.nexblocks.authguard.service.keys;

import org.bouncycastle.crypto.digests.Blake2bDigest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class Blake2bApiKeyHash implements ApiKeyHash {

    private final byte[] hashKey;
    private final int digestSize;

    public Blake2bApiKeyHash(final String hashKey, final int digestSize) {
        this.hashKey = hashKey.getBytes(StandardCharsets.UTF_8);
        this.digestSize = digestSize;
    }

    @Override
    public String hash(final String apiKey) {
        final byte[] output = hashToBytes(apiKey);

        return Base64.getEncoder().encodeToString(output);
    }

    @Override
    public boolean verify(final String plain, final String hashed) {
        final byte[] storedHashBytes = Base64.getDecoder().decode(hashed);
        final byte[] hashedBytes = hashToBytes(plain);

        return Arrays.equals(storedHashBytes, hashedBytes);
    }

    private byte[] hashToBytes(final String message) {
        final Blake2bDigest digest = new Blake2bDigest(hashKey, digestSize, null, null);
        final byte[] apiKeyBytes = message.getBytes(StandardCharsets.UTF_8);
        final byte[] output = new byte[digestSize];

        digest.update(apiKeyBytes, 0, apiKeyBytes.length);
        digest.doFinal(output, 0);

        return output;
    }
}
