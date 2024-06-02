package com.nexblocks.authguard.crypto.generators;

import com.nexblocks.authguard.crypto.GeneratorResult;

import java.security.SecureRandom;

public class ChaCha20Generator implements Generator<ChaCha20Parameters> {
    private final SecureRandom secureRandom;

    public ChaCha20Generator() {
        secureRandom = new SecureRandom();
    }

    @Override
    public GeneratorResult generate(final ChaCha20Parameters parameters) {
        final byte[] key = new byte[32];

        secureRandom.nextBytes(key);

        return GeneratorResult.symmetric(key);
    }
}
