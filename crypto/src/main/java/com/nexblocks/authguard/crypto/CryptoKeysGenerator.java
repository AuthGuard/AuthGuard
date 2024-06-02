package com.nexblocks.authguard.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class CryptoKeysGenerator {
    public static SecretKey generateAes(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("AES");
        keyGenerator.init(n);

        return keyGenerator.generateKey();
    }

    public static KeyPair generateRsa(int n) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeySpecException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(512);

        final KeyPair keys = generator.genKeyPair();

        final byte[] publicKey = keys.getPublic().getEncoded();
        final byte[] privateKey = keys.getPrivate().getEncoded();

        final String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey);
        final String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey);

        return AsymmetricKeys.rsaFromBase64Keys(publicKeyBase64, privateKeyBase64);
    }
}
