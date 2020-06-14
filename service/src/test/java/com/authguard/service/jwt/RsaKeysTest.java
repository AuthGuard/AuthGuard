package com.authguard.service.jwt;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class RsaKeysTest {

    @Test
    void fromBase64Keys256() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(512);

        final KeyPair keys = generator.genKeyPair();

        final byte[] publicKey = keys.getPublic().getEncoded();
        final byte[] privateKey = keys.getPrivate().getEncoded();

        final String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey);
        final String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey);

        final KeyPair actual = RsaKeys.fromBase64Keys(publicKeyBase64, privateKeyBase64);

        assertThat(actual.getPublic().getEncoded()).isEqualTo(publicKey);
        assertThat(actual.getPrivate().getEncoded()).isEqualTo(privateKey);
    }
}