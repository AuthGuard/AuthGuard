package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.TotpKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TotpKeyRequestValidatorTest {
    @Test
    void validateValid() {
        TotpKeyRequestDTO request = TotpKeyRequestDTO.builder()
                .accountId(1)
                .build();

        Validator<TotpKeyRequestDTO> validator = Validators.getForClass(TotpKeyRequestDTO.class);

        List<Violation> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validateMissingAccountId() {
        TotpKeyRequestDTO request = TotpKeyRequestDTO.builder()
                .build();

        Validator<TotpKeyRequestDTO> validator = Validators.getForClass(TotpKeyRequestDTO.class);

        List<Violation> violations = validator.validate(request);

        assertThat(violations).containsExactly(
                new Violation("accountId", ViolationType.MISSING_REQUIRED_VALUE)
        );
    }
}