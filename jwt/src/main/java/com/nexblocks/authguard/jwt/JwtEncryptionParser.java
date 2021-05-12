package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.service.config.EncryptionConfig;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;

import javax.crypto.Cipher;
import java.util.Optional;

public class JwtEncryptionParser {

    static Cipher parseCipher(final EncryptionConfig encryptionConfig) {
        return parseAlgorithm(encryptionConfig.getAlgorithm())
                .flatMap(Cryptography::getCipher)
                .orElseThrow(() -> new ConfigurationException("Invalid encryption algorithm " + encryptionConfig.getAlgorithm()));
    }

    static Optional<Cryptography.Algorithm> parseAlgorithm(final String algorithm) {
        try {
            return Optional.of(Cryptography.Algorithm.valueOf(algorithm));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
