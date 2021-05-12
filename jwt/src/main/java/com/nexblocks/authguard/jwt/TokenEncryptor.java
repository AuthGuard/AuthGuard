package com.nexblocks.authguard.jwt;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.EncryptionConfig;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import io.vavr.control.Either;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Encrypts and decrypts tokens, only if token encryption is enabled. It uses
 * {@link Cryptography} to do the actual encryption and decryption. When it's
 * initialized, it'll attempt to parse the encryption configuration and to
 * read the keys from disk. If either of those steps failed, it will fail to
 * initialize.
 */
@Singleton
public class TokenEncryptor {
    private final boolean enabled;
    private final Cipher cipher;
    private final KeyPair keyPair;

    @Inject
    public TokenEncryptor(final @Named("jwt") ConfigContext configContext) {
        this(configContext.asConfigBean(JwtConfig.class));
    }

    public TokenEncryptor(final JwtConfig jwtConfig) {
        final EncryptionConfig encryptionConfig = jwtConfig.getEncryption();

        if (encryptionConfig != null) {
            this.enabled = true;

            final Cryptography.Algorithm algorithm = JwtEncryptionParser.parseAlgorithm(encryptionConfig.getAlgorithm())
                    .orElseThrow(() -> new ConfigurationException("Invalid encryption algorithm " + encryptionConfig.getAlgorithm()));

            this.cipher = JwtEncryptionParser.parseCipher(encryptionConfig);
            this.keyPair = readKeys(algorithm, encryptionConfig.getPublicKey(), encryptionConfig.getPrivateKey());
        } else {
            this.enabled = false;
            this.cipher = null;
            this.keyPair = null;
        }
    }

    /**
     * Encrypts a token and gives back the result as a base64 string.
     * @param token The token to encrypt. Must be UTF-8 encoded.
     * @return A base64 encrypted text of the token.
     */
    public Either<Exception, String> encryptAndEncode(final String token) {
        if (this.enabled) {
            final byte[] raw = token.getBytes(StandardCharsets.UTF_8);
            final byte[] encrypted = Cryptography.encrypt(raw, cipher, keyPair.getPublic());
            final String encoded = Base64.getEncoder().encodeToString(encrypted);

            return Either.right(encoded);
        } else {
            return Either.left(new ServiceException(ErrorCode.ENCRYPTION_NOT_SUPPORTED, "JWT encryption is not enabled"));
        }
    }

    /**
     * Decrypts a token which was encrypted and base64 encoded.
     * @param encryptedToken Encrypted and base64-encoded string.
     * @return The decrypted token, UTF-8 encoded.
     */
    public Either<Exception, String> decryptEncoded(final String encryptedToken) {
        if (this.enabled) {
            final byte[] decoded = Base64.getDecoder().decode(encryptedToken);
            final byte[] decrypted = Cryptography.decrypt(decoded, cipher, keyPair.getPrivate());

            return Either.right(new String(decrypted, StandardCharsets.UTF_8));
        } else {
            return Either.left(new ServiceException(ErrorCode.ENCRYPTION_NOT_SUPPORTED, "JWT encryption is not enabled"));
        }
    }

    private KeyPair readKeys(final Cryptography.Algorithm algorithm,
                             final String publicKeyPath, final String privateKeyPath) {
        final byte[] publicKeyBase64 = KeyLoader.readPemKeyFile(publicKeyPath);
        final byte[] privateKeyBase64 = KeyLoader.readPemKeyFile(privateKeyPath);

        return readKeysForFail(algorithm, publicKeyBase64, privateKeyBase64);
    }

    private KeyPair readKeysForFail(final Cryptography.Algorithm algorithm, final byte[] publicKeyBase64,
                                    final byte[] privateKeyBase64) {
        try {
            switch (algorithm) {
                case RSA:
                    return AsymmetricKeys.rsaFromBase64Keys(publicKeyBase64, privateKeyBase64);

                case EC:
                    return AsymmetricKeys.eciesFromBase64Keys(publicKeyBase64, privateKeyBase64);

                default:
                    throw new ConfigurationException("Unsupported algorithm " + algorithm);
            }
        } catch (final InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new ConfigurationException("Failed to read keys", e);
        }
    }
}
