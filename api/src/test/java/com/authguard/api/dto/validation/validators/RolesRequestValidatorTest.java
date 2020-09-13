package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.requests.RolesRequest;
import com.authguard.api.dto.requests.RolesRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RolesRequestValidatorTest {

    @Test
    void validateValid() {
        final RolesRequestDTO request = RolesRequestDTO.builder()
                .action(RolesRequest.Action.GRANT)
                .addRoles("test")
                .build();

        final Validator<RolesRequestDTO> validator = Validators.getForClass(RolesRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateNoFields() {
        final RolesRequestDTO request = RolesRequestDTO.builder()
                .build();

        final Validator<RolesRequestDTO> validator = Validators.getForClass(RolesRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                new Violation("roles", ViolationType.EMPTY_LIST),
                new Violation("action", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}