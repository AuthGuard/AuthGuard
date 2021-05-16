package com.nexblocks.authguard.jwt.crypto;

public interface TokenEncryptor {
    /**
     * Encrypts a token and gives back the result as a single base64 string,
     * or as multiple base64 strings separated by '.'
     * @param token The token to encrypt. Must be UTF-8 encoded.
     * @return A base64 encrypted text of the token.
     */
    String encryptAndEncode(final String token);

    /**
     * Decrypts a token which was encrypted and base64 encoded.
     * @param encryptedToken Encrypted and base64-encoded string.
     * @return The decrypted token, UTF-8 encoded.
     */
    String decryptEncoded(final String encryptedToken);
}
