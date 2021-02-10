package com.nexblocks.authguard.api.dto.validation.basic;

import com.nexblocks.authguard.api.dto.validation.Constraint;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;

import java.util.Optional;

public class NonBlankStringValidator implements Constraint<String> {

    @Override
    public Optional<Violation> validate(final String fieldName, final String value) {
        if (value != null) {
            if (value.isBlank()) {
                final Violation violation = new Violation(fieldName, ViolationType.INVALID_VALUE);

                return Optional.of(violation);
            }
        }
        return Optional.empty();
    }
}
