package com.authguard.service.passwords;

import org.bouncycastle.crypto.generators.SCrypt;

public class SCryptPassword extends AbstractSecurePassword {
    private static final int CPU_MEMORY_COST_PARAM = 2;
    private static final int BLOCK_SIZE = 1;
    private static final int PARALLELIZATION_PARAM = 1;
    private static final int SALT_SIZE = 32;

    public SCryptPassword() {
        super(SALT_SIZE);
    }

    @Override
    protected byte[] hashWithSalt(final String plain, final byte[] saltBytes) {
        return SCrypt.generate(plain.getBytes(), saltBytes, CPU_MEMORY_COST_PARAM, BLOCK_SIZE, PARALLELIZATION_PARAM, 50);
    }
}
