package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.ApiKeyRequestDTO;
import com.nexblocks.authguard.api.dto.requests.DurationRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.dto.validation.violations.Violations;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeysRequestValidatorTest {

    @Test
    void validate() {
        final ApiKeyRequestDTO request = ApiKeyRequestDTO.builder()
                .appId(1L)
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

    @Test
    void validateWithZeroValidity() {
        final ApiKeyRequestDTO request = ApiKeyRequestDTO.builder()
                .appId(1L)
                .keyType("default")
                .validFor(DurationRequestDTO.builder()
                        .build())
                .build();

        final Validator<ApiKeyRequestDTO> validator = Validators.getForClass(ApiKeyRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateWithNonZeroValidity() {
        final ApiKeyRequestDTO request = ApiKeyRequestDTO.builder()
                .appId(1L)
                .keyType("default")
                .validFor(DurationRequestDTO.builder()
                        .days(1)
                        .minutes(1)
                        .hours(1)
                        .build())
                .build();

        final Validator<ApiKeyRequestDTO> validator = Validators.getForClass(ApiKeyRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateWithNegativeDaysValidity() {
        final ApiKeyRequestDTO request = ApiKeyRequestDTO.builder()
                .appId(1L)
                .keyType("default")
                .validFor(DurationRequestDTO.builder()
                        .days(-1)
                        .build())
                .build();

        final Validator<ApiKeyRequestDTO> validator = Validators.getForClass(ApiKeyRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                Violations.invalidValue("validFor.days")
        );
    }

    @Test
    void validateWithNegativeHoursValidity() {
        final ApiKeyRequestDTO request = ApiKeyRequestDTO.builder()
                .appId(1L)
                .keyType("default")
                .validFor(DurationRequestDTO.builder()
                        .hours(-1)
                        .build())
                .build();

        final Validator<ApiKeyRequestDTO> validator = Validators.getForClass(ApiKeyRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                Violations.invalidValue("validFor.hours")
        );
    }

    @Test
    void validateWithNegativeMinutesValidity() {
        final ApiKeyRequestDTO request = ApiKeyRequestDTO.builder()
                .appId(1L)
                .keyType("default")
                .validFor(DurationRequestDTO.builder()
                        .minutes(-1)
                        .build())
                .build();

        final Validator<ApiKeyRequestDTO> validator = Validators.getForClass(ApiKeyRequestDTO.class);
        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                Violations.invalidValue("validFor.minutes")
        );
    }
}