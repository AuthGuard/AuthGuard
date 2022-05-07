package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.ApiKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeysRequestValidatorTest {

    @Test
    void validate() {
        final ApiKeyRequestDTO request = ApiKeyRequestDTO.builder()
                .appId("app")
                .keyType("default")
                .build();

        final Validator<ApiKeyRequestDTO> validator = Validators.getForClass(ApiKeyRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingValues() {
        final ApiKeyRequestDTO request = ApiKeyRequestDTO.builder()
                .build();

        final Validator<ApiKeyRequestDTO> validator = Validators.getForClass(ApiKeyRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                new Violation("appId", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("keyType", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}