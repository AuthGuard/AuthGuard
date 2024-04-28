package com.nexblocks.authguard.crypto.generators;

import com.nexblocks.authguard.crypto.GeneratorResult;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.StringWriter;
import java.io.Writer;
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
