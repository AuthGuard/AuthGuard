package com.nexblocks.authguard.crypto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.nexblocks.authguard.crypto.generators.*;

import java.util.Collections;
import java.util.Map;

public class Algorithms {
    public static final AlgorithmDetails<AesParameters> aes =
            new AlgorithmDetails<>("AES", AlgorithmDetails.Type.SYMMETRIC,
                    Sets.newHashSet(64, 128, 256), new AesGenerator());

    public static final AlgorithmDetails<RsaParameters> rsa =
            new AlgorithmDetails<>("RSA", AlgorithmDetails.Type.ASYMMETRIC,
                    Sets.newHashSet(256, 512, 1024, 2048), new RsaGenerator());

    public static final AlgorithmDetails<EcSecp256k1Parameters> ecSecp256k1 =
            new AlgorithmDetails<>("ECC_secp256k1", AlgorithmDetails.Type.ASYMMETRIC,
                    Sets.newHashSet(256, 512, 1024, 2048), new EcSecp256k1Generator());

    public static final AlgorithmDetails<ChaCha20Parameters> chaCha20 =
            new AlgorithmDetails<>("ChaCha20", AlgorithmDetails.Type.SYMMETRIC,
                    Collections.singleton(32), new ChaCha20Generator());

    public static final Map<String, AlgorithmDetails<?>> detailsByName =
            ImmutableMap.<String, AlgorithmDetails<?>>builder()
                    .put(aes.getName(), aes)
                    .put(rsa.getName(), rsa)
                    .put(ecSecp256k1.getName(), ecSecp256k1)
                    .put(chaCha20.getName(), chaCha20)
                    .build();
}
