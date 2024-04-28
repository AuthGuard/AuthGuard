package com.nexblocks.authguard.crypto;

public class GeneratorResult {
    private final byte[] privateKey;
    private final byte[] publicKey;

    public GeneratorResult(final byte[] privateKey, final byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public static GeneratorResult symmetric(final byte[] key) {
        return new GeneratorResult(key, null);
    }

    public static GeneratorResult asymmetric(final byte[] privateKey, final byte[] publicKey) {
        return new GeneratorResult(privateKey, publicKey);
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}
