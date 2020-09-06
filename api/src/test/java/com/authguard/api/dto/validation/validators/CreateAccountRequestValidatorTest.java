package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.AccountEmailDTO;
import com.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAccountRequestValidatorTest {

    @Test
    void validateValidNoFields() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateValidFields() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .externalId("external")
                .addEmails(AccountEmailDTO.builder()
                        .email("valid@valid.com")
                        .build())
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateInvalidEmail() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .externalId("external")
                .addEmails(AccountEmailDTO.builder()
                        .email("invalid")
                        .build())
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("email", ViolationType.INVALID_VALUE));
    }
}