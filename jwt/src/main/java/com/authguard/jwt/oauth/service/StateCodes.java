package com.authguard.jwt.oauth.service;

import java.math.BigInteger;
import java.security.SecureRandom;

public class StateCodes {
    private static final SecureRandom random = new SecureRandom();

    public static String create() {
        return new BigInteger(130, random).toString(32);
    }
}
