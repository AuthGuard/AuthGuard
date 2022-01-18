package com.nexblocks.authguard.basic.passwords;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.basic.config.PasswordsConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.ConfigParser;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SecurePasswordProvider {
    private final SecurePassword securePassword;
    private final boolean expirePasswords;
    private final Duration passwordTtl;
    private final Integer currentVersion;
    private final Integer minimumVersion;

    private Map<Integer, SecurePassword> previousVersions;

    @Inject
    public SecurePasswordProvider(final @Named("passwords") ConfigContext config) {
        final PasswordsConfig passwordsConfig = config.asConfigBean(PasswordsConfig.class);

        this.securePassword = parsePasswordConfiguration(passwordsConfig);
        this.currentVersion = passwordsConfig.getVersion();
        this.minimumVersion = passwordsConfig.getMinimumVersion();

        if (passwordsConfig.getValidFor() != null) {
            expirePasswords = true;
            passwordTtl = ConfigParser.parseDuration(passwordsConfig.getValidFor());
        } else {
            expirePasswords = false;
            passwordTtl = null;
        }

        if (passwordsConfig.getPreviousVersions() != null) {
            this.previousVersions = passwordsConfig.getPreviousVersions().stream()
                    .collect(Collectors.toMap(PasswordsConfig::getVersion, this::parsePasswordConfiguration));
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

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public Integer getMinimumVersion() {
        return minimumVersion;
    }

    public Map<Integer, SecurePassword> getPreviousVersions() {
        return previousVersions;
    }

    private SecurePassword parsePasswordConfiguration(final PasswordsConfig passwordsConfig) {
        switch (passwordsConfig.getAlgorithm()) {
            case "scrypt":
                return new SCryptPassword(passwordsConfig.getScrypt());

            case "bcrypt":
                return new BCryptPassword(passwordsConfig.getBcrypt());

            default:
                throw new IllegalStateException("Unsupported password algorithm " + passwordsConfig.getAlgorithm());
        }
    }
}
