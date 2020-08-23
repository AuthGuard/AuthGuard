package com.authguard.service.passwords;

import com.authguard.config.ConfigContext;
import com.authguard.service.config.BCryptConfig;
import com.authguard.service.config.PasswordsConfig;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.bouncycastle.crypto.generators.BCrypt;

public class BCryptPassword extends AbstractSecurePassword {
    private static final int COST = 4;
    private static final int SALT_SIZE = 16; // 128 bits

    private final BCryptConfig config;

    public BCryptPassword() {
        this(BCryptConfig.builder().build());
    }

    public BCryptPassword(final BCryptConfig config) {
        super(config.getSaltSize());
        this.config = config;
    }

    @Inject
    public BCryptPassword(final @Named("passwords") ConfigContext config) {
        this(config.asConfigBean(PasswordsConfig.class).getBcrypt());
    }

    @Override
    protected byte[] hashWithSalt(final String plain, final byte[] saltBytes) {
        return BCrypt.generate(plain.getBytes(), saltBytes, COST);
    }
}
