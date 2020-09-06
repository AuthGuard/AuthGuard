package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.PermissionDTO;
import com.authguard.api.dto.requests.PermissionsRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionsRequestValidatorTest {

    @Test
    void validateValid() {
        final PermissionsRequestDTO request = PermissionsRequestDTO.builder()
                .addPermissions(PermissionDTO.builder()
                        .group("group")
                        .name("*")
                        .build())
                .build();

        final Validator<PermissionsRequestDTO> validator = Validators.getForClass(PermissionsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateNoPermissions() {
        final PermissionsRequestDTO request = PermissionsRequestDTO.builder()
                .build();

        final Validator<PermissionsRequestDTO> validator = Validators.getForClass(PermissionsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("permissions", ViolationType.EMPTY_LIST));
    }

    @Test
    void validateInvalidPermission() {
        final PermissionsRequestDTO request = PermissionsRequestDTO.builder()
                .addPermissions(PermissionDTO.builder()
                        .build())
                .build();

        final Validator<PermissionsRequestDTO> validator = Validators.getForClass(PermissionsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactlyInAnyOrder(
                new Violation("group", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("name", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}