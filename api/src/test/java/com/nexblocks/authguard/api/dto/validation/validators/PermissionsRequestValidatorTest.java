package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.requests.PermissionsRequest;
import com.nexblocks.authguard.api.dto.requests.PermissionsRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionsRequestValidatorTest {

    @Test
    void validateValid() {
        final PermissionsRequestDTO request = PermissionsRequestDTO.builder()
                .action(PermissionsRequest.Action.GRANT)
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
    void validateNoFields() {
        final PermissionsRequestDTO request = PermissionsRequestDTO.builder()
                .build();

        final Validator<PermissionsRequestDTO> validator = Validators.getForClass(PermissionsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                new Violation("permissions", ViolationType.EMPTY_LIST),
                new Violation("action", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }

    @Test
    void validateInvalidPermission() {
        final PermissionsRequestDTO request = PermissionsRequestDTO.builder()
                .action(PermissionsRequest.Action.GRANT)
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