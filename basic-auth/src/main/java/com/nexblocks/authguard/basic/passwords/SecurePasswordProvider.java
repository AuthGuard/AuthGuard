package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.PasswordsConfig;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SecurePasswordProvider {
    private final SecurePassword securePassword;

    @Inject
    public SecurePasswordProvider(final @Named("passwords") ConfigContext config) {
        final PasswordsConfig passwordsConfig = config.asConfigBean(PasswordsConfig.class);

        switch (passwordsConfig.getAlgorithm()) {
            case "scrypt":
                this.securePassword = new SCryptPassword(passwordsConfig.getScrypt());
                break;

            case "bcrypt":
                this.securePassword = new BCryptPassword(passwordsConfig.getBcrypt());
                break;

            default:
                throw new IllegalStateException("Unsupported password algorithm " + passwordsConfig.getAlgorithm());
        }
    }

    public SecurePassword get() {
        return securePassword;
    }
}
