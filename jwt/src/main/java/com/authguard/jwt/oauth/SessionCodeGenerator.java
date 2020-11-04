package com.authguard.jwt.oauth;

import java.math.BigInteger;
import java.security.SecureRandom;

public class SessionCodeGenerator {
    private final SecureRandom secureRandom;

    public SessionCodeGenerator() {
        secureRandom = new SecureRandom();
    }

    private String generate() {
        return new BigInteger(130, secureRandom).toString();
    }
}
