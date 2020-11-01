package com.authguard.service.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class ID {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public static String generate() {
        byte[] buffer = new byte[20];
        secureRandom.nextBytes(buffer);
        return encoder.encodeToString(buffer);
    }
}
