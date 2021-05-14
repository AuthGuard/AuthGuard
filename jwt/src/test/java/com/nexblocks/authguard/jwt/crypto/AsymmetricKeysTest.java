package com.nexblocks.authguard.jwt.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AsymmetricKeysTest {

    @BeforeEach
    void setProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void rsaFromBase64Keys512() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(512);

        final KeyPair keys = generator.genKeyPair();

        final byte[] publicKey = keys.getPublic().getEncoded();
        final byte[] privateKey = keys.getPrivate().getEncoded();

        final String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey);
        final String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey);

        final KeyPair actual = AsymmetricKeys.rsaFromBase64Keys(publicKeyBase64, privateKeyBase64);

        assertThat(actual.getPublic().getEncoded()).isEqualTo(publicKey);
        assertThat(actual.getPrivate().getEncoded()).isEqualTo(privateKey);
    }

    @Test
    void rsaFromBase64Keys512Bytes() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(512);

        final KeyPair keys = generator.genKeyPair();

        final byte[] publicKey = keys.getPublic().getEncoded();
        final byte[] privateKey = keys.getPrivate().getEncoded();

        final KeyPair actual = AsymmetricKeys.rsaFromBase64Keys(publicKey, privateKey);

        assertThat(actual.getPublic().getEncoded()).isEqualTo(publicKey);
        assertThat(actual.getPrivate().getEncoded()).isEqualTo(privateKey);
    }

    @Test
    void ecdsaFromBase64Keys256() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDSA", "BC");
        generator.initialize(256);

        final KeyPair keys = generator.genKeyPair();

        final byte[] publicKey = keys.getPublic().getEncoded();
        final byte[] privateKey = keys.getPrivate().getEncoded();

        final String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey);
        final String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey);

        final KeyPair actual = AsymmetricKeys.ecdsaFromBase64Keys(publicKeyBase64, privateKeyBase64);

        assertThat(actual.getPublic().getEncoded()).isEqualTo(publicKey);
        assertThat(actual.getPrivate().getEncoded()).isEqualTo(privateKey);
    }

    @Test
    void ecdsaFromBase64Keys256Bytes() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDSA", "BC");
        generator.initialize(256);

        final KeyPair keys = generator.genKeyPair();

        final byte[] publicKey = keys.getPublic().getEncoded();
        final byte[] privateKey = keys.getPrivate().getEncoded();

        final KeyPair actual = AsymmetricKeys.ecdsaFromBase64Keys(publicKey, privateKey);

        assertThat(actual.getPublic().getEncoded()).isEqualTo(publicKey);
        assertThat(actual.getPrivate().getEncoded()).isEqualTo(privateKey);
    }
}