package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.PasswordResetRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetRequestValidatorTest {

    @Test
    void validateValidByToken() {
        final PasswordResetRequestDTO request = PasswordResetRequestDTO.builder()
                .byToken(true)
                .resetToken("token")
                .newPassword("newPassword")
                .domain("main")
                .build();

        final Validator<PasswordResetRequestDTO> validator = Validators.getForClass(PasswordResetRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingTokenAndNewPassword() {
        final PasswordResetRequestDTO request = PasswordResetRequestDTO.builder()
                .byToken(true)
                .build();

        final Validator<PasswordResetRequestDTO> validator = Validators.getForClass(PasswordResetRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("resetToken", ViolationType.MISSING_REQUIRED_VALUE));
        assertThat(violations).contains(new Violation("newPassword", ViolationType.MISSING_REQUIRED_VALUE));
    }

    @Test
    void validateValidByPassword() {
        final PasswordResetRequestDTO request = PasswordResetRequestDTO.builder()
                .byToken(false)
                .identifier("identifier")
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .domain("main")
                .build();

        final Validator<PasswordResetRequestDTO> validator = Validators.getForClass(PasswordResetRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingIdentifierAndOldPassword() {
        final PasswordResetRequestDTO request = PasswordResetRequestDTO.builder()
                .byToken(false)
                .build();

        final Validator<PasswordResetRequestDTO> validator = Validators.getForClass(PasswordResetRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("identifier", ViolationType.MISSING_REQUIRED_VALUE));
        assertThat(violations).contains(new Violation("oldPassword", ViolationType.MISSING_REQUIRED_VALUE));
        assertThat(violations).contains(new Violation("newPassword", ViolationType.MISSING_REQUIRED_VALUE));
    }
}