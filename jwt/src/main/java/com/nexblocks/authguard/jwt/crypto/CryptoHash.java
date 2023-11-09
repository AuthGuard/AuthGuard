package com.nexblocks.authguard.jwt.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoHash {
    public static String hash(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            md.update(plain.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();

            return String.format("%064x", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
