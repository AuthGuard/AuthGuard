package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.TokenRestrictionsDTO;
import com.authguard.api.dto.requests.AuthRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestValidatorTest {

    @Test
    void validateNoViolationsNoRestrictions() {
        final AuthRequestDTO valid = AuthRequestDTO.builder()
                .authorization("authorization")
                .build();

        final Validator<AuthRequestDTO> validator = Validators.getForClass(AuthRequestDTO.class);
        final List<Violation> violations = validator.validate(valid);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateNoViolations() {
        final AuthRequestDTO valid = AuthRequestDTO.builder()
                .authorization("authorization")
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
    void validateNoAuthorization() {
        final AuthRequestDTO valid = AuthRequestDTO.builder()
                .build();

        final Validator<AuthRequestDTO> validator = Validators.getForClass(AuthRequestDTO.class);
        final List<Violation> violations = validator.validate(valid);

        assertThat(violations).contains(new Violation("authorization", ViolationType.MISSING_REQUIRED_VALUE));
    }
}