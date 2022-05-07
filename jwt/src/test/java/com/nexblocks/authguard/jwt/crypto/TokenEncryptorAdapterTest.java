package com.nexblocks.authguard.jwt.crypto;

import com.nexblocks.authguard.service.config.EncryptionConfig;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptorAdapterTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void encryptAndDecryptEllipticCurve() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("EC")
                        .publicKey("file:src/test/resources/ec256-public.pem")
                        .privateKey("file:src/test/resources/ec256-private.pem")
                        .build())
                .build();

        final TokenEncryptorAdapter encryptor = new TokenEncryptorAdapter(jwtConfig);

        final String encrypted = encryptor.encryptAndEncode(TOKEN).get();
        final String decrypted = encryptor.decryptEncoded(encrypted).get();

        assertThat(decrypted).isEqualTo(TOKEN);
    }

    @Test
    void encryptAndDecryptAes() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("AES_CBC")
                        .privateKey("file:src/test/resources/aes128.txt")
                        .build())
                .build();

        final TokenEncryptorAdapter encryptor = new TokenEncryptorAdapter(jwtConfig);

        final String encrypted = encryptor.encryptAndEncode(TOKEN).get();
        final String decrypted = encryptor.decryptEncoded(encrypted).get();

        assertThat(decrypted).isEqualTo(TOKEN);
    }

    @Test
    void encryptAndDecryptNotEnabled() {
        final JwtConfig jwtConfig = JwtConfig.builder().build();

        final TokenEncryptorAdapter encryptor = new TokenEncryptorAdapter(jwtConfig);

        assertThat(encryptor.encryptAndEncode("").isLeft()).isTrue();
        assertThat(encryptor.decryptEncoded("").isLeft()).isTrue();
    }

    @Test
    void initializeWithUnsupportedAlgorithm() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("unknown")
                        .build())
                .build();

        assertThatThrownBy(() -> new TokenEncryptorAdapter(jwtConfig)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void initializeWithNonExistingKeys() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("EC")
                        .publicKey("file:src/test/resources/none.pem")
                        .privateKey("file:src/test/resources/none.pem")
                        .build())
                .build();

        assertThatThrownBy(() -> new TokenEncryptorAdapter(jwtConfig)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initializeWithWrongKeys() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("EC")
                        .publicKey("file:src/test/resources/hmac256.pem")
                        .privateKey("file:src/test/resources/hmac256.pem")
                        .build())
                .build();

        assertThatThrownBy(() -> new TokenEncryptorAdapter(jwtConfig)).isInstanceOf(ConfigurationException.class);
    }
}