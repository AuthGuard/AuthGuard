package com.nexblocks.authguard.crypto;

import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class ChaCha20Encryptor {
    public static byte[] encrypt(byte[] data, byte[] key, byte[] nonce) {
        ChaCha7539Engine engine = new ChaCha7539Engine();
        KeyParameter keyParam = new KeyParameter(key);

        engine.init(true, new ParametersWithIV(keyParam, nonce));

        byte[] output = new byte[data.length];

        engine.processBytes(data, 0, data.length, output, 0);

        return output;
    }

    public static byte[] decrypt(byte[] encrypted, byte[] key, byte[] nonce) {
        ChaCha7539Engine engine = new ChaCha7539Engine();
        KeyParameter keyParam = new KeyParameter(key);

        engine.init(true, new ParametersWithIV(keyParam, nonce));

        byte[] output = new byte[encrypted.length];

        engine.processBytes(encrypted, 0, encrypted.length, output, 0);

        return output;
    }
}
