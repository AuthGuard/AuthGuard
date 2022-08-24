package com.nexblocks.authguard.jwt.crypto;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

public class EciesTokenEncryptor implements TokenEncryptor {
    private final KeyPair keyPair;

    public EciesTokenEncryptor(final String publicKeyPath, final String privateKeyPath) {
        keyPair = KeysReader.readKeyPair(Cryptography.Algorithm.EC, publicKeyPath, privateKeyPath);
    }

    /**
     * Encrypts a token and gives back the result as a base64 string.
     * @param token The token to encrypt. Must be UTF-8 encoded.
     * @return A base64 encrypted text of the token.
     */
    public String encryptAndEncode(final String token) {
        final Cipher cipher = Cryptography.createCipher(Cryptography.Algorithm.EC,
                Cipher.ENCRYPT_MODE, keyPair.getPublic());

        final byte[] raw = token.getBytes(StandardCharsets.UTF_8);
        final byte[] encrypted = Cryptography.doCipher(raw, cipher);

        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts a token which was encrypted and base64 encoded.
     * @param encryptedToken Encrypted and base64-encoded string.
     * @return The decrypted token, UTF-8 encoded.
     */
    public String decryptEncoded(final String encryptedToken) {
        final Cipher cipher = Cryptography.createCipher(Cryptography.Algorithm.EC,
                Cipher.DECRYPT_MODE, keyPair.getPrivate());

        final byte[] decoded = Base64.getDecoder().decode(encryptedToken);
        final byte[] decrypted = Cryptography.doCipher(decoded, cipher);

        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
