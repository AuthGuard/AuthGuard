package com.nexblocks.authguard.jwt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Optional;

/**
 * A class to hide some of the complexity of the java security
 * API.
 */
public class Cryptography {

    private static final String SECURITY_PROVIDER = "BC";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    enum Algorithm {
        EC("ECIES");

        private final String value;

        Algorithm(final String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    static Optional<Cipher> getCipher(final Algorithm algorithm) {
        try {
            return Optional.of(Cipher.getInstance(algorithm.value(), SECURITY_PROVIDER));
        } catch (final NoSuchAlgorithmException e) {
            return Optional.empty();
        } catch (final NoSuchProviderException | NoSuchPaddingException e) {
            throw new IllegalStateException(e); // if we reached this point then we're doomed
        }
    }

    static byte[] encrypt(final byte[] data, final Cipher cipher, final Key key) {
        return cipher(data, cipher, key, Cipher.ENCRYPT_MODE);
    }

    static byte[] decrypt(final byte[] data, final Cipher cipher, final Key key) {
        return cipher(data, cipher, key, Cipher.DECRYPT_MODE);
    }

    private static byte[] cipher(final byte[] data, final Cipher cipher, final Key key, final int mode) {
        try {
            cipher.init(mode, key);
            return cipher.doFinal(data);
        } catch (final InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException(e);
        }
    }
}
