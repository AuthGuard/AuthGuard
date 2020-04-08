package com.authguard.service.passwords;

import org.bouncycastle.crypto.generators.BCrypt;

public class BCryptPassword extends AbstractSecurePassword {
    private static final int COST = 4;
    private static final int SALT_SIZE = 16; // 128 bits

    public BCryptPassword() {
        super(SALT_SIZE);
    }

    @Override
    protected byte[] hashWithSalt(final String plain, final byte[] saltBytes) {
        return BCrypt.generate(plain.getBytes(), saltBytes, COST);
    }
}
