package com.authguard.bindings;

import com.authguard.config.ConfigContext;
import com.authguard.basic.passwords.BCryptPassword;
import com.authguard.basic.passwords.SCryptPassword;
import com.authguard.basic.passwords.SecurePassword;
import com.google.inject.AbstractModule;

public class PasswordsBinder extends AbstractModule {
    private final ConfigContext configContext;

    public PasswordsBinder(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public void configure() {
        if (configContext.get("passwords") == null) {
            throw new IllegalStateException("Passwords configuration must be provided");
        }

        final ConfigContext passwordsConfig = configContext.getSubContext("passwords");
        final String algorithm = passwordsConfig.getAsString("algorithm");

        if (algorithm == null) {
            throw new IllegalStateException("Configuration parameter passwords.algorithm is required");
        }

        switch (algorithm.toLowerCase()) {
            case "scrypt":
                bind(SecurePassword.class).to(SCryptPassword.class);
                break;

            case "bcrypt":
                bind(SecurePassword.class).to(BCryptPassword.class);
                break;

            default:
                throw new IllegalStateException("Unsupported password hashing algorithm " + algorithm);
        }
    }
}
