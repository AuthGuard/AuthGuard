package com.nexblocks.authguard.jwt.crypto;

import com.nexblocks.authguard.service.exceptions.ServiceException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesCbcTokenEncryptorTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void encryptAndDecrypt() {
        final AesCbcTokenEncryptor encryptor = new AesCbcTokenEncryptor("file:src/test/resources/aes128.txt");

        final String encrypted = encryptor.encryptAndEncode(TOKEN);
        final String decrypted = encryptor.decryptEncoded(encrypted);

        assertThat(decrypted).isEqualTo(TOKEN);
    }

    @Test
    void decryptInvalidFormat() {
        final AesCbcTokenEncryptor encryptor = new AesCbcTokenEncryptor("file:src/test/resources/aes128.txt");

        assertThatThrownBy(() -> encryptor.decryptEncoded("totally wrong"))
                .isInstanceOf(ServiceException.class);
    }
}