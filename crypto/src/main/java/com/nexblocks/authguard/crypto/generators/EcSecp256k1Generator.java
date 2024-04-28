package com.nexblocks.authguard.crypto.generators;

import com.nexblocks.authguard.crypto.GeneratorResult;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import java.security.*;
import java.util.Optional;
import java.util.stream.Stream;

public class EcSecp256k1Generator implements Generator<EcSecp256k1Parameters> {
    static {
        Optional<Provider> bc = Stream.of(Security.getProviders())
                .filter(provider -> "BC".equals(provider.getName()))
                .findFirst();

        if (bc.isEmpty()) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public GeneratorResult generate(final EcSecp256k1Parameters parameters) {
        try {
            ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(ecSpec);

            KeyPair keys = keyPairGenerator.generateKeyPair();

            byte[] publicKey = keys.getPublic().getEncoded();
            byte[] privateKey = keys.getPrivate().getEncoded();

            return GeneratorResult.asymmetric(privateKey, publicKey);

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
