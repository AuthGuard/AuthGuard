package com.nexblocks.authguard.api.dto.validation.basic;

import com.nexblocks.authguard.api.dto.validation.Constraint;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;

import java.util.Optional;

public class Required implements Constraint {
    @Override
    public Optional<Violation> validate(final String fieldName, final Object value) {
        return value == null ? Optional.of(new Violation(fieldName, ViolationType.MISSING_REQUIRED_VALUE))
                : Optional.empty();
    }
}
