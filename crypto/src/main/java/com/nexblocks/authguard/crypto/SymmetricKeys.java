package com.nexblocks.authguard.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class SymmetricKeys {
    public static SecretKey aesFromBase64Key(final String base64Key) {
        final byte[] decodedKey = Base64.getDecoder().decode(base64Key);

        return aesFromBase64Key(decodedKey);
    }

    public static SecretKey aesFromBase64Key(final byte[] base64Key) {
        return new SecretKeySpec(base64Key, "AES");
    }
}
