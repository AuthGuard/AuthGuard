package com.nexblocks.authguard.jwt.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

class EciesTokenEncryptorTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void encryptAndDecrypt() {
        final EciesTokenEncryptor encryptor = new EciesTokenEncryptor(
                "file:src/test/resources/ec256-public.pem",
                "file:src/test/resources/ec256-private.pem");

        final String encrypted = encryptor.encryptAndEncode(TOKEN);
        final String decrypted = encryptor.decryptEncoded(encrypted);

        assertThat(decrypted).isEqualTo(TOKEN);
    }
}