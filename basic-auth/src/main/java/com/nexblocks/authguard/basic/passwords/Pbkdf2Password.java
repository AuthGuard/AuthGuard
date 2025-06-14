package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.basic.config.Pbkdf2Config;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import io.smallrye.mutiny.Uni;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class Pbkdf2Password extends AbstractSecurePassword {
    private final String pbkdf2Algorithm;
    private final int iterations;
    private final int derivedKeySize;

    public Pbkdf2Password(Pbkdf2Config config) {
        super(config.getSaltSize());

        this.iterations = config.getIterations();
        switch (config.getHashingAlgorithm()) {
            case SHA_256:
                this.pbkdf2Algorithm = "PBKDF2WithHmacSHA256";
                this.derivedKeySize = 256;
                break;

            case SHA_512:
                this.pbkdf2Algorithm = "PBKDF2WithHmacSHA512";
                this.derivedKeySize = 512;
                break;

            default:
                throw new ConfigurationException("Unknown PBKDF2 hashing algorithm "
                        + config.getHashingAlgorithm());
        }
    }

    @Override
    protected Uni<byte[]> hashWithSalt(String plain, byte[] saltBytes) {
        KeySpec spec = new PBEKeySpec(plain.toCharArray(), saltBytes,
                iterations, derivedKeySize);

        return Uni.createFrom().item(() -> {
            try {
                return getSecretKeyFactory().generateSecret(spec).getEncoded();
            } catch (InvalidKeySpecException e) {
                throw new RuntimeException("Credential could not be encoded", e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private SecretKeyFactory getSecretKeyFactory() {
        try {
            Provider bcFipsProvider = Security.getProvider("BC");
            return SecretKeyFactory.getInstance(pbkdf2Algorithm, bcFipsProvider);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("PBKDF2 algorithm not found", e);
        }
    }
}
