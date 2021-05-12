package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.service.config.EncryptionConfig;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptorTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void encryptAndDecryptRsa() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("RSA")
                        .publicKey("src/test/resources/rsa512-public.pem")
                        .privateKey("src/test/resources/rsa512-private.pem")
                        .build())
                .build();

        final TokenEncryptor encryptor = new TokenEncryptor(jwtConfig);

        final String encrypted = encryptor.encryptAndEncode(TOKEN).get();
        final String decrypted = encryptor.decryptEncoded(encrypted).get();

        assertThat(decrypted).isEqualTo(TOKEN);
    }

    @Test
    void encryptAndDecryptEllipticCurve() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("EC")
                        .publicKey("src/test/resources/ec256-public.pem")
                        .privateKey("src/test/resources/ec256-private.pem")
                        .build())
                .build();

        final TokenEncryptor encryptor = new TokenEncryptor(jwtConfig);

        final String encrypted = encryptor.encryptAndEncode(TOKEN).get();
        final String decrypted = encryptor.decryptEncoded(encrypted).get();

        assertThat(decrypted).isEqualTo(TOKEN);
    }

    @Test
    void encryptAndDecryptNotEnabled() {
        final JwtConfig jwtConfig = JwtConfig.builder().build();

        final TokenEncryptor encryptor = new TokenEncryptor(jwtConfig);

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

        assertThatThrownBy(() -> new TokenEncryptor(jwtConfig)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void initializeWithNonExistingKeys() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("RSA")
                        .publicKey("src/test/resources/none.pem")
                        .privateKey("src/test/resources/none.pem")
                        .build())
                .build();

        assertThatThrownBy(() -> new TokenEncryptor(jwtConfig)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initializeWithWrongKeys() {
        final JwtConfig jwtConfig = JwtConfig.builder()
                .encryption(EncryptionConfig.builder()
                        .algorithm("RSA")
                        .publicKey("src/test/resources/hmac256.pem")
                        .privateKey("src/test/resources/hmac256.pem")
                        .build())
                .build();

        assertThatThrownBy(() -> new TokenEncryptor(jwtConfig)).isInstanceOf(ConfigurationException.class);
    }
}