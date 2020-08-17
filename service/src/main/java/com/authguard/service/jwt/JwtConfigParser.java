package com.authguard.service.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.codes.ErrorCode;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

public class JwtConfigParser {

    public static Algorithm parseAlgorithm(final String algorithmName, final String publicKey,
                                           final String privateKey) {
        if (algorithmName.startsWith("HMAC")) {
            return parseHmac(algorithmName, privateKey);
        } else if (algorithmName.startsWith("RSA")) {
            return parseRsa(algorithmName, publicKey, privateKey);
        } else if (algorithmName.startsWith("ECDSA")) {
            return parseEc(algorithmName, publicKey, privateKey);
        } else {
            throw new ServiceException(ErrorCode.UNSUPPORTED_JWT_ALGORITHM, "Unsupported algorithm " + algorithmName);
        }
    }

    private static Algorithm parseHmac(final String algorithmName, final String key) {
        switch (algorithmName) {
            case "HMAC256":
                return Algorithm.HMAC256(key);

            case "HMAC512":
                return Algorithm.HMAC512(key);

            default:
                throw new ServiceException(ErrorCode.UNSUPPORTED_JWT_ALGORITHM, "Unsupported algorithm " + algorithmName);
        }
    }

    private static Algorithm parseRsa(final String algorithmName, final String publicKey,
                                      final String privateKey) {
        final KeyPair keyPair = readRsaKeys(publicKey, privateKey);

        switch (algorithmName) {
            case "RSA256":
                return Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());

            case "RSA512":
                return Algorithm.RSA512((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());

            default:
                throw new ServiceException(ErrorCode.UNSUPPORTED_JWT_ALGORITHM, "Unsupported algorithm " + algorithmName);
        }
    }

    private static Algorithm parseEc(final String algorithmName, final String publicKey,
                                     final String privateKey) {
        final KeyPair keyPair = readEcKeys(publicKey, privateKey);

        switch (algorithmName) {
            case "RSA256":
                return Algorithm.ECDSA256((ECPublicKey) keyPair.getPublic(), (ECPrivateKey) keyPair.getPrivate());

            case "RSA512":
                return Algorithm.ECDSA512((ECPublicKey) keyPair.getPublic(), (ECPrivateKey) keyPair.getPrivate());

            default:
                throw new ServiceException(ErrorCode.UNSUPPORTED_JWT_ALGORITHM, "Unsupported algorithm " + algorithmName);
        }
    }

    private static KeyPair readRsaKeys(final String publicKey, final String privateKey) {
        try {
            return AsymmetricKeys.rsaFromBase64Keys(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyPair readEcKeys(final String publicKey, final String privateKey) {
        try {
            return AsymmetricKeys.ecdsaFromBase64Keys(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
