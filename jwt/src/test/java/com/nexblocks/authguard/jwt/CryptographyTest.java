package com.nexblocks.authguard.jwt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import static org.assertj.core.api.Assertions.assertThat;

class CryptographyTest {

    @BeforeEach
    void setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void getCipher() {
        assertThat(Cryptography.getCipher(Cryptography.Algorithm.RSA)).isPresent();
        assertThat(Cryptography.getCipher(Cryptography.Algorithm.EC)).isPresent();
    }

    @Test
    void encryptAndDecryptRsa() throws InvalidKeySpecException, NoSuchAlgorithmException {
        final String publicKeyPath = "src/test/resources/rsa512-public.pem";
        final String privateKeyPath = "src/test/resources/rsa512-private.pem";

        final byte[] publicKey = KeyLoader.readPemKeyFile(publicKeyPath);
        final byte[] privateKey = KeyLoader.readPemKeyFile(privateKeyPath);

        final KeyPair keyPair = AsymmetricKeys.rsaFromBase64Keys(publicKey, privateKey);
        final Cipher cipher = Cryptography.getCipher(Cryptography.Algorithm.RSA).orElseThrow();

        final String text = "This is a text to test encryption and decryption";

        final byte[] encrypted = Cryptography.encrypt(text.getBytes(), cipher, keyPair.getPublic());
        final byte[] decrypted = Cryptography.decrypt(encrypted, cipher, keyPair.getPrivate());

        assertThat(decrypted).isEqualTo(text.getBytes());
    }

    @Test
    void encryptAndDecryptEllipticCurve() throws InvalidKeySpecException, NoSuchAlgorithmException {
        final String publicKeyPath = "src/test/resources/ec256-public.pem";
        final String privateKeyPath = "src/test/resources/ec256-private.pem";

        final byte[] publicKey = KeyLoader.readPemKeyFile(publicKeyPath);
        final byte[] privateKey = KeyLoader.readPemKeyFile(privateKeyPath);

        final KeyPair keyPair = AsymmetricKeys.eciesFromBase64Keys(publicKey, privateKey);
        final Cipher cipher = Cryptography.getCipher(Cryptography.Algorithm.EC).orElseThrow();

        final String text = "This is a text to test encryption and decryption";

        final byte[] encrypted = Cryptography.encrypt(text.getBytes(), cipher, keyPair.getPublic());
        final byte[] decrypted = Cryptography.decrypt(encrypted, cipher, keyPair.getPrivate());

        assertThat(decrypted).isEqualTo(text.getBytes());
    }
}