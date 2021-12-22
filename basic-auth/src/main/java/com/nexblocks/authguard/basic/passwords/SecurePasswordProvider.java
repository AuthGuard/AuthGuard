package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.basic.config.PasswordsConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.service.config.ConfigParser;

import java.time.Duration;

public class SecurePasswordProvider {
    private final SecurePassword securePassword;
    private final boolean expirePasswords;
    private final Duration passwordTtl;

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

        if (passwordsConfig.getValidFor() != null) {
            expirePasswords = true;
            passwordTtl = ConfigParser.parseDuration(passwordsConfig.getValidFor());
        } else {
            expirePasswords = false;
            passwordTtl = null;
        }
    }

    public SecurePassword get() {
        return securePassword;
    }

    public boolean passwordsExpire() {
        return expirePasswords;
    }

    public Duration getPasswordTtl() {
        return passwordTtl;
    }
}
