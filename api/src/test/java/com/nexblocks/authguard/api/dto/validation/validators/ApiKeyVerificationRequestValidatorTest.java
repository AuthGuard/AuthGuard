package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.ApiKeyVerificationRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyVerificationRequestValidatorTest {

    @Test
    void validate() {
        final ApiKeyVerificationRequestDTO request = ApiKeyVerificationRequestDTO.builder()
                .key("key")
                .keyType("default")
                .build();

        final Validator<ApiKeyVerificationRequestDTO> validator = Validators.getForClass(ApiKeyVerificationRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingValues() {
        final ApiKeyVerificationRequestDTO request = ApiKeyVerificationRequestDTO.builder()
                .build();

        final Validator<ApiKeyVerificationRequestDTO> validator = Validators.getForClass(ApiKeyVerificationRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                new Violation("key", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("keyType", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}