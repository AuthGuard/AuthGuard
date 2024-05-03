package com.nexblocks.authguard.crypto.generators;

import com.nexblocks.authguard.crypto.GeneratorResult;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.util.Optional;
import java.util.stream.Stream;

public class RsaGenerator implements Generator<RsaParameters> {

    static {
        Optional<Provider> bc = Stream.of(Security.getProviders())
                .filter(provider -> "BC".equals(provider.getName()))
                .findFirst();

        if (bc.isEmpty()) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public GeneratorResult generate(final RsaParameters parameters) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
            generator.initialize(512);

            KeyPair keys = generator.genKeyPair();

            byte[] publicKey = keys.getPublic().getEncoded();
            byte[] privateKey = keys.getPrivate().getEncoded();

            return GeneratorResult.asymmetric(privateKey, publicKey);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
