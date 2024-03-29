package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.AccountEmailDTO;
import com.nexblocks.authguard.api.dto.entities.UserIdentifier;
import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAccountRequestValidatorTest {

    @Test
    void validateValidNoFields() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .domain("main")
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                new Violation("identifiers", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }

    @Test
    void validateNoDomain() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactlyInAnyOrder(
                new Violation("identifiers", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("domain", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }

    @Test
    void validateValidEmail() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .externalId("external")
                .email(AccountEmailDTO.builder()
                        .email("valid@valid.com")
                        .build())
                .domain("main")
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateInvalidEmail() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .externalId("external")
                .email(AccountEmailDTO.builder()
                        .email("invalid")
                        .build())
                .domain("main")
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("email", ViolationType.INVALID_VALUE));
    }

    @Test
    void validateValidBackupEmail() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .externalId("external")
                .backupEmail(AccountEmailDTO.builder()
                        .email("valid@valid.com")
                        .build())
                .addIdentifiers(UserIdentifierDTO.builder()
                        .type(UserIdentifier.Type.USERNAME)
                        .identifier("username")
                        .build())
                .domain("main")
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateInvalidBackupEmail() {
        final CreateAccountRequestDTO request = CreateAccountRequestDTO.builder()
                .externalId("external")
                .backupEmail(AccountEmailDTO.builder()
                        .email("invalid")
                        .build())
                .domain("main")
                .build();

        final Validator<CreateAccountRequestDTO> validator = Validators.getForClass(CreateAccountRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).contains(new Violation("backupEmail", ViolationType.INVALID_VALUE));
    }
}