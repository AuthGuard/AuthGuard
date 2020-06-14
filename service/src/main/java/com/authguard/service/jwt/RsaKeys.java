package com.authguard.service.jwt;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaKeys {
    public static KeyPair fromBase64Keys(final String base64Public, final String base64Private) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        final byte[] privateKey = Base64.getDecoder().decode(base64Private);
        final PKCS8EncodedKeySpec privateKeySpecs = new PKCS8EncodedKeySpec(privateKey);

        final byte[] publicKey = Base64.getDecoder().decode(base64Public);
        final X509EncodedKeySpec publicKeySpecs = new X509EncodedKeySpec(publicKey);

        return new KeyPair(keyFactory.generatePublic(publicKeySpecs), keyFactory.generatePrivate(privateKeySpecs));
    }
}
