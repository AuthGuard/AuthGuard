package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CreatePermissionRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreatePermissionRequestValidatorTest {
    @Test
    void validateValidRequest() {
        final CreatePermissionRequestDTO request = CreatePermissionRequestDTO.builder()
                .group("group")
                .name("test-Permission")
                .domain("main")
                .build();

        final Validator<CreatePermissionRequestDTO> validator = Validators.getForClass(CreatePermissionRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateNoFields() {
        final CreatePermissionRequestDTO request = CreatePermissionRequestDTO.builder()
                .build();

        final Validator<CreatePermissionRequestDTO> validator = Validators.getForClass(CreatePermissionRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactlyInAnyOrder(
                new Violation("name", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("group", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("domain", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}
