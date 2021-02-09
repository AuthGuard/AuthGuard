package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.UserIdentifier;
import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.CreateCredentialsRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCredentialsRequestValidatorTest {

    @Test
    void validateValid() {
        final CreateCredentialsRequestDTO request = CreateCredentialsRequestDTO.builder()
                .accountId("account")
                .addIdentifiers(UserIdentifierDTO.builder()
                        .type(UserIdentifier.Type.USERNAME)
                        .identifier("username")
                        .build())
                .plainPassword("password")
                .build();

        final Validator<CreateCredentialsRequestDTO> validator = Validators.getForClass(CreateCredentialsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingFields() {
        final CreateCredentialsRequestDTO request = CreateCredentialsRequestDTO.builder()
                .build();

        final Validator<CreateCredentialsRequestDTO> validator = Validators.getForClass(CreateCredentialsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactlyInAnyOrder(
                new Violation("accountId", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("plainPassword", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("identifiers", ViolationType.EMPTY_LIST)
        );
    }

    @Test
    void validateInvalidIdentifierFields() {
        final CreateCredentialsRequestDTO request = CreateCredentialsRequestDTO.builder()
                .accountId("account")
                .addIdentifiers(UserIdentifierDTO.builder()
                        .build())
                .plainPassword("password")
                .build();

        final Validator<CreateCredentialsRequestDTO> validator = Validators.getForClass(CreateCredentialsRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactlyInAnyOrder(
                new Violation("type", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("identifier", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}