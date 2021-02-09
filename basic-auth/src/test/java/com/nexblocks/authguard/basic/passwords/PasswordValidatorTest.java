package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.service.config.PasswordConditions;
import com.nexblocks.authguard.service.config.PasswordsConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    @Test
    void allConditionsSet() {
        final PasswordConditions conditions = PasswordConditions.builder()
                .includeCaps(true)
                .minLength(6)
                .maxLength(12)
                .includeSpecialCharacters(true)
                .includeDigits(true)
                .build();
        final PasswordValidator validator = new PasswordValidator(PasswordsConfig.builder()
                .conditions(conditions)
                .build());

        final String passesAll = "SecurePass2#";
        final String failsCapsCheck = "securepass2#";
        final String failsSmallsCheck = "SECUREPASS2#";
        final String failsSpecialCharsCheck = "SecurePass2";
        final String failsDigitCheck = "SecurePass#";
        final String failsMinLengthCheck = "S!#1s";
        final String failsMaxLengthCheck = "SecurePass2#SecurePass2#";

        assertThat(validator.findViolations(passesAll)).isEmpty();

        validateViolationTypes(validator.findViolations(failsCapsCheck), Violation.Type.NOT_ENOUGH_CAPITAL_LETTERS);
        validateViolationTypes(validator.findViolations(failsSmallsCheck), Violation.Type.NOT_ENOUGH_SMALL_LETTERS);
        validateViolationTypes(validator.findViolations(failsSpecialCharsCheck), Violation.Type.NOT_ENOUGH_SPECIAL_CHARS);
        validateViolationTypes(validator.findViolations(failsDigitCheck), Violation.Type.NOT_ENOUGH_DIGITS);
        validateViolationTypes(validator.findViolations(failsMinLengthCheck), Violation.Type.INVALID_SIZE);
        validateViolationTypes(validator.findViolations(failsMaxLengthCheck), Violation.Type.INVALID_SIZE);
    }

    @Test
    void noConditionsSet() {
        final PasswordConditions conditions = PasswordConditions.builder()
                .minLength(0)
                .includeSmallLetters(false)
                .build();

        final PasswordValidator validator = new PasswordValidator(PasswordsConfig.builder()
                .conditions(conditions)
                .build());

        assertThat(validator.findViolations("")).isEmpty();
    }

    private void validateViolationTypes(final List<Violation> violations, final Violation.Type... expected) {
        final List<Violation.Type> actual = violations.stream()
                .map(Violation::getType)
                .collect(Collectors.toList());

        Assertions.assertThat(actual).containsExactlyInAnyOrder(expected);
    }
}