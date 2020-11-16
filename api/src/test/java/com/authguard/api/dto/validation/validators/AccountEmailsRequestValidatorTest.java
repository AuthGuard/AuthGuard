package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.AccountEmailDTO;
import com.authguard.api.dto.requests.AccountEmailsRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountEmailsRequestValidatorTest {

    @Test
    void validateValid() {
        final AccountEmailsRequestDTO request = AccountEmailsRequestDTO.builder()
                .email(AccountEmailDTO.builder()
                        .email("valid@valid.com")
                        .build())
                .build();

        final Validator<AccountEmailsRequestDTO> validator = Validators.getForClass(AccountEmailsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateInvalidEmail() {
        final AccountEmailsRequestDTO request = AccountEmailsRequestDTO.builder()
                .email(AccountEmailDTO.builder()
                        .email("invalid")
                        .build())
                .build();

        final Validator<AccountEmailsRequestDTO> validator = Validators.getForClass(AccountEmailsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(new Violation("email", ViolationType.INVALID_VALUE));
    }
}