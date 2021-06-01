package com.nexblocks.authguard.jwt.crypto;

import com.nexblocks.authguard.service.exceptions.ConfigurationException;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class KeysReader {
    static KeyPair readKeyPair(final Cryptography.Algorithm algorithm,
                               final String publicKeyPath, final String privateKeyPath) {
        final byte[] publicKeyBase64 = KeyLoader.readPemKeyFile(publicKeyPath);
        final byte[] privateKeyBase64 = KeyLoader.readPemKeyFile(privateKeyPath);

        return readKeyPairForFail(algorithm, publicKeyBase64, privateKeyBase64);
    }

    static KeyPair readKeyPairForFail(final Cryptography.Algorithm algorithm, final byte[] publicKeyBase64,
                                      final byte[] privateKeyBase64) {
        try {
            if (algorithm == Cryptography.Algorithm.EC) {
                return AsymmetricKeys.eciesFromBase64Keys(publicKeyBase64, privateKeyBase64);
            }

            throw new ConfigurationException("Unsupported algorithm " + algorithm);
        } catch (final InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new ConfigurationException("Failed to read keys", e);
        }
    }

    static SecretKey readSecretKey(final Cryptography.Algorithm algorithm, final String keyPath) {
        final byte[] keyBase64 = KeyLoader.readTextKeyFile(keyPath);

        return readSecretKeyOrFail(algorithm, keyBase64);
    }

    static SecretKey readSecretKeyOrFail(final Cryptography.Algorithm algorithm, final byte[] keyBase64) {
        if (algorithm == Cryptography.Algorithm.AES_CBC) {
            return SymmetricKeys.aesFromBase64Key(keyBase64);
        }

        throw new ConfigurationException("Unsupported algorithm " + algorithm);
    }
}