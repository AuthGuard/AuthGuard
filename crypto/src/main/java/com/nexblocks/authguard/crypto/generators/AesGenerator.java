package com.nexblocks.authguard.crypto.generators;

import com.nexblocks.authguard.crypto.GeneratorResult;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

public class AesGenerator implements Generator<AesParameters> {
    @Override
    public GeneratorResult generate(final AesParameters parameters) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

            keyGenerator.init(parameters.getSize());

            SecretKey key = keyGenerator.generateKey();

            return new GeneratorResult(key.getEncoded(), null);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
