package com.nexblocks.authguard.jwt.crypto;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.EncryptionConfig;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import io.vavr.control.Either;

/**
 * Encrypts and decrypts tokens, only if token encryption is enabled. It uses
 * {@link TokenEncryptor} to do the actual encryption and decryption. When it's
 * initialized, it'll attempt to parse the encryption configuration and to
 * read the keys from disk. If either of those steps failed, it will fail to
 * initialize.
 */
public class TokenEncryptorAdapter {
    private final boolean enabled;
    private final TokenEncryptor tokenEncryptor;

    @Inject
    public TokenEncryptorAdapter(final @Named("jwt") ConfigContext configContext) {
        this(configContext.asConfigBean(JwtConfig.class));
    }

    public TokenEncryptorAdapter(final JwtConfig jwtConfig) {
        final EncryptionConfig encryptionConfig = jwtConfig.getEncryption();

        if (encryptionConfig != null) {
            this.enabled = true;

            final Cryptography.Algorithm algorithm = JwtEncryptionParser.parseAlgorithm(encryptionConfig.getAlgorithm())
                    .orElseThrow(() -> new ConfigurationException("Invalid encryption algorithm " + encryptionConfig.getAlgorithm()));

            switch (algorithm) {
                case EC:
                    tokenEncryptor = new EciesTokenEncryptor(encryptionConfig.getPublicKey(), encryptionConfig.getPrivateKey());
                    break;

                case AES_CBC:
                    tokenEncryptor = new AesCbcTokenEncryptor(encryptionConfig.getPrivateKey());
                    break;

                default:
                    throw new ConfigurationException("Unsupported algorithm " + algorithm);
            }
        } else {
            this.enabled = false;
            this.tokenEncryptor = null;
        }
    }

    /**
     * Encrypts a token and gives back the result as a base64 string.
     * @param token The token to encrypt. Must be UTF-8 encoded.
     * @return A right Either of the encrypted token, if encryption was
     *          enabled. A left either with a {@link ServiceException}
     *          otherwise.
     */
    public Either<Exception, String> encryptAndEncode(final String token) {
        if (this.enabled) {
            return Either.right(tokenEncryptor.encryptAndEncode(token));
        } else {
            return Either.left(new ServiceException(ErrorCode.ENCRYPTION_NOT_SUPPORTED, "JWT encryption is not enabled"));
        }
    }

    /**
     * Decrypts a token which was encrypted and base64 encoded.
     * @param encryptedToken Encrypted and base64-encoded string.
     * @return A right Either of the decrypted token, if encryption was
     *      enabled. A left either with a {@link ServiceException}
     *      otherwise.
     */
    public Either<Exception, String> decryptEncoded(final String encryptedToken) {
        if (this.enabled) {
            return Either.right(tokenEncryptor.decryptEncoded(encryptedToken));
        } else {
            return Either.left(new ServiceException(ErrorCode.ENCRYPTION_NOT_SUPPORTED, "JWT encryption is not enabled"));
        }
    }
}
