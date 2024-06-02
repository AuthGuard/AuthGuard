package com.nexblocks.authguard.crypto;

import com.nexblocks.authguard.crypto.SymmetricKeys;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SymmetricKeysTest {

    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);

        return keyGenerator.generateKey();
    }

    @Test
    void aesFromBase64Key() throws NoSuchAlgorithmException {
        final SecretKey key = generateKey(128);
        final String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());

        final SecretKey converted = SymmetricKeys.aesFromBase64Key(base64Key);

        assertThat(converted).isEqualTo(key);
    }
}