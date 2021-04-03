package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.AccountEmailDTO;
import com.nexblocks.authguard.api.dto.requests.UpdateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateAccountRequestValidatorTest {
    @Test
    void validateValidNoFields() {
        final UpdateAccountRequestDTO request = UpdateAccountRequestDTO.builder()
                .build();

        final Validator<UpdateAccountRequestDTO> validator = Validators.getForClass(UpdateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateValidEmail() {
        final UpdateAccountRequestDTO request = UpdateAccountRequestDTO.builder()
                .email(AccountEmailDTO.builder()
                        .email("valid@valid.com")
                        .build())
                .build();

        final Validator<UpdateAccountRequestDTO> validator = Validators.getForClass(UpdateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateInvalidEmail() {
        final UpdateAccountRequestDTO request = UpdateAccountRequestDTO.builder()
                .email(AccountEmailDTO.builder()
                        .email("invalid")
                        .build())
                .build();

        final Validator<UpdateAccountRequestDTO> validator = Validators.getForClass(UpdateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("email", ViolationType.INVALID_VALUE));
    }

    @Test
    void validateValidBackupEmail() {
        final UpdateAccountRequestDTO request = UpdateAccountRequestDTO.builder()
                .backupEmail(AccountEmailDTO.builder()
                        .email("valid@valid.com")
                        .build())
                .build();

        final Validator<UpdateAccountRequestDTO> validator = Validators.getForClass(UpdateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateInvalidBackupEmail() {
        final UpdateAccountRequestDTO request = UpdateAccountRequestDTO.builder()
                .backupEmail(AccountEmailDTO.builder()
                        .email("invalid")
                        .build())
                .build();

        final Validator<UpdateAccountRequestDTO> validator = Validators.getForClass(UpdateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("backupEmail", ViolationType.INVALID_VALUE));
    }
}
