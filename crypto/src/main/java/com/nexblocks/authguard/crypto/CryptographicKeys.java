package com.nexblocks.authguard.crypto;

public class CryptographicKeys {
    private final AlgorithmDetails algorithm;
    private final byte[] privateKey;
    private final byte[] publicKey;

    public CryptographicKeys(final AlgorithmDetails algorithm, final byte[] privateKey, final byte[] publicKey) {
        this.algorithm = algorithm;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public AlgorithmDetails getAlgorithm() {
        return algorithm;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}
