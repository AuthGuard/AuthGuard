package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CreateClientRequest;
import com.nexblocks.authguard.api.dto.requests.CreateClientRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateClientRequestValidatorTest {

    @Test
    void validateValid() {
        final CreateClientRequestDTO request = CreateClientRequestDTO.builder()
                .name("app")
                .domain("main")
                .clientType(CreateClientRequest.ClientType.AUTH)
                .build();

        final Validator<CreateClientRequestDTO> validator = Validators.getForClass(CreateClientRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingNameAndDomain() {
        final CreateClientRequestDTO request = CreateClientRequestDTO.builder()
                .build();

        final Validator<CreateClientRequestDTO> validator = Validators.getForClass(CreateClientRequestDTO.class);

        final List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactlyInAnyOrder(
                new Violation("name", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("domain", ViolationType.MISSING_REQUIRED_VALUE),
                new Violation("clientType", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}