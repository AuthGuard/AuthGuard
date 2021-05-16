package com.nexblocks.authguard.jwt.crypto;

import java.util.Optional;

public class JwtEncryptionParser {

    public static Optional<Cryptography.Algorithm> parseAlgorithm(final String algorithm) {
        try {
            return Optional.of(Cryptography.Algorithm.valueOf(algorithm));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
