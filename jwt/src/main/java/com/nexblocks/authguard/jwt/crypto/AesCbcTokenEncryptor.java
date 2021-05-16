package com.nexblocks.authguard.jwt.crypto;


import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.random.CryptographicRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AesCbcTokenEncryptor implements TokenEncryptor {
    private final SecretKey secretKey;
    private final CryptographicRandom random;

    public AesCbcTokenEncryptor(final String keyPath) {
        this.secretKey = KeysReader.readSecretKey(Cryptography.Algorithm.AES_CBC, keyPath);
        this.random = new CryptographicRandom();
    }

    /**
     * Encrypts a token and gives back the result as a base64 string.
     * @param token The token to encrypt. Must be UTF-8 encoded.
     * @return A base64 encrypted text of the token.
     */
    public String encryptAndEncode(final String token) {
        final byte[] ivBytes = generateIv();
        final Cipher cipher = Cryptography.createCipherWithIv(Cryptography.Algorithm.AES_CBC,
                Cipher.ENCRYPT_MODE, secretKey, ivBytes);

        final byte[] raw = token.getBytes(StandardCharsets.UTF_8);
        final byte[] encrypted = Cryptography.doCipher(raw, cipher);
        final String encoded = Base64.getEncoder().encodeToString(encrypted);

        return Base64.getEncoder().encodeToString(ivBytes) + '.' + encoded;
    }

    /**
     * Decrypts a token which was encrypted and base64 encoded.
     * @param encryptedToken Encrypted and base64-encoded string. Must be
     *                       in the format {IV}.{encrypted_token}
     * @return The decrypted token, UTF-8 encoded.
     */
    public String decryptEncoded(final String encryptedToken) {
        final String[] parts = encryptedToken.split("\\.");

        if (parts.length != 2) {
            throw new ServiceException(ErrorCode.INVALID_TOKEN, "Invalid encrypted token");
        }

        final String ivBase64 = parts[0];
        final String tokenBase64 = parts[1];

        final byte[] ivBytes = Base64.getDecoder().decode(ivBase64);

        final Cipher cipher = Cryptography.createCipherWithIv(Cryptography.Algorithm.AES_CBC,
                Cipher.DECRYPT_MODE, secretKey, ivBytes);

        final byte[] decoded = Base64.getDecoder().decode(tokenBase64);
        final byte[] decrypted = Cryptography.doCipher(decoded, cipher);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private byte[] generateIv() {
        return random.bytes(16);
    }
}
