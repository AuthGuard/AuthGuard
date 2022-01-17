package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.TokenRestrictionsDTO;
import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestValidatorTest {

    @Test
    void validateNoViolationsNoRestrictions() {
        final AuthRequestDTO valid = AuthRequestDTO.builder()
                .build();

        final Validator<AuthRequestDTO> validator = Validators.getForClass(AuthRequestDTO.class);
        final List<Violation> violations = validator.validate(valid);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateNoViolations() {
        final AuthRequestDTO valid = AuthRequestDTO.builder()
                .restrictions(TokenRestrictionsDTO.builder()
                        .addScopes("scope")
                        .addPermissions("permission")
                        .build())
                .build();

        final Validator<AuthRequestDTO> validator = Validators.getForClass(AuthRequestDTO.class);
        final List<Violation> violations = validator.validate(valid);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateWithIdentifierAndDomain() {
        final AuthRequestDTO valid = AuthRequestDTO.builder()
                .identifier("identifier")
                .domain("main")
                .build();

        final Validator<AuthRequestDTO> validator = Validators.getForClass(AuthRequestDTO.class);
        final List<Violation> violations = validator.validate(valid);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateWithIdentifierButNoDomain() {
        final AuthRequestDTO valid = AuthRequestDTO.builder()
                .identifier("identifier")
                .build();

        final Validator<AuthRequestDTO> validator = Validators.getForClass(AuthRequestDTO.class);
        final List<Violation> violations = validator.validate(valid);

        assertThat(violations).containsExactly(new Violation("domain", ViolationType.MISSING_REQUIRED_VALUE));
    }
}