package com.nexblocks.authguard.jwt.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.security.Security;

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
        EC("ECIES"),
        AES_CBC("AES/CBC/PKCS5Padding");

        private final String value;

        Algorithm(final String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    static Cipher createCipher(final Algorithm algorithm, final int mode, final Key key) {
        try {
            final Cipher cipher = Cipher.getInstance(algorithm.value(), SECURITY_PROVIDER);

            cipher.init(mode, key);

            return cipher;
        } catch (final Exception e) {
            throw new IllegalStateException(e); // we should have done enough validation to never reach this point
        }
    }

    static Cipher createCipherWithIv(final Algorithm algorithm, final int mode,
                                     final SecretKey key, final byte[] ivBytes) {
        final IvParameterSpec iv = new IvParameterSpec(ivBytes);

        try {
            final Cipher cipher = Cipher.getInstance(algorithm.value(), SECURITY_PROVIDER);

            cipher.init(mode, key, iv);

            return cipher;
        } catch (final Exception e) {
            throw new IllegalStateException(e); // we should have done enough validation to never reach this point
        }
    }

    static byte[] doCipher(final byte[] data, final Cipher cipher) {
        try {
            return cipher.doFinal(data);
        } catch (final BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException(e);
        }
    }
}
