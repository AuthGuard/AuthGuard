package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.PasswordlessRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordlessRequestValidatorTest {

    @Test
    void validateValid() {
        final PasswordlessRequestDTO request = PasswordlessRequestDTO.builder()
                .token("token")
                .build();

        final Validator<PasswordlessRequestDTO> validator = Validators.getForClass(PasswordlessRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingFields() {
        final PasswordlessRequestDTO request = PasswordlessRequestDTO.builder()
                .build();

        final Validator<PasswordlessRequestDTO> validator = Validators.getForClass(PasswordlessRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("token", ViolationType.MISSING_REQUIRED_VALUE));
    }
}