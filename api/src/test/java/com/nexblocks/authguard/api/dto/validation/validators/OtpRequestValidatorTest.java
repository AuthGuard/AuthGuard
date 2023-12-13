package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.OtpRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OtpRequestValidatorTest {

    @Test
    void validateValid() {
        final OtpRequestDTO request = OtpRequestDTO.builder()
                .passwordId(1L)
                .password("password")
                .build();

        final Validator<OtpRequestDTO> validator = Validators.getForClass(OtpRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingFields() {
        final OtpRequestDTO request = OtpRequestDTO.builder()
                .build();

        final Validator<OtpRequestDTO> validator = Validators.getForClass(OtpRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactlyInAnyOrder(
                new Violation("password", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("passwordId", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}