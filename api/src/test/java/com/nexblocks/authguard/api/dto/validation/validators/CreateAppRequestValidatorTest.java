package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CreateAppRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAppRequestValidatorTest {

    @Test
    void validateValid() {
        final CreateAppRequestDTO request = CreateAppRequestDTO.builder()
                .name("app")
                .domain("main")
                .build();

        final Validator<CreateAppRequestDTO> validator = Validators.getForClass(CreateAppRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingNameAndDomain() {
        final CreateAppRequestDTO request = CreateAppRequestDTO.builder()
                .build();

        final Validator<CreateAppRequestDTO> validator = Validators.getForClass(CreateAppRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactlyInAnyOrder(
                new Violation("name", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("domain", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}