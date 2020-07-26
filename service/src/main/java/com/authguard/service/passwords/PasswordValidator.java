package com.authguard.service.passwords;

import com.authguard.config.ConfigContext;
import com.authguard.service.config.PasswordsConfig;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.ArrayList;
import java.util.List;

public class PasswordValidator {
    private final PasswordsConfig config;

    @Inject
    public PasswordValidator(final @Named("passwords") ConfigContext config) {
        this.config = config.asConfigBean(PasswordsConfig.class);
    }

    public PasswordValidator(final PasswordsConfig config) {
        this.config = config;
    }

    public List<Violation> findViolations(final String password) {
        final PasswordCharacteristics characteristics = new PasswordCharacteristics(password);
        final List<Violation> violations = new ArrayList<>();

        if (config.getConditions().includeDigits() && !characteristics.containsDigits()) {
            violations.add(new Violation(Violation.Type.NOT_ENOUGH_DIGITS, "Must contain digits"));
        }

        if (config.getConditions().includeSpecialCharacters() &&!characteristics.containsSpecialCharacters()) {
            violations.add(new Violation(Violation.Type.NOT_ENOUGH_SPECIAL_CHARS, "Must contain special characters"));
        }

        if (config.getConditions().includeSmallLetters() && !characteristics.containsSmallLetters()) {
            violations.add(new Violation(Violation.Type.NOT_ENOUGH_SMALL_LETTERS, "Must contain lower case letters"));
        }

        if (config.getConditions().includeCaps() && !characteristics.containsCapitalLetters()) {
            violations.add(new Violation(Violation.Type.NOT_ENOUGH_CAPITAL_LETTERS, "Must contain upper case letters"));
        }

        if (password.length() < config.getConditions().getMinLength()
                || password.length() > config.getConditions().getMaxLength()) {
            final String message = "Must be between " + config.getConditions().getMinLength() + " and " +
                    config.getConditions().getMaxLength() + " characters long";
            violations.add(new Violation(Violation.Type.INVALID_SIZE, message));
        }

        return violations;
    }
}
