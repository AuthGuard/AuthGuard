package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CreateRoleRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateRoleRequestValidatorTest {

    @Test
    void validateValidRequest() {
        final CreateRoleRequestDTO request = CreateRoleRequestDTO.builder()
                .name("test-role")
                .domain("main")
                .forAccounts(true)
                .forApplications(false)
                .build();

        final Validator<CreateRoleRequestDTO> validator = Validators.getForClass(CreateRoleRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateNoValues() {
        final CreateRoleRequestDTO request = CreateRoleRequestDTO.builder()
                .build();

        final Validator<CreateRoleRequestDTO> validator = Validators.getForClass(CreateRoleRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                new Violation("name", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("domain", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("forAccounts", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("forApplications", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}