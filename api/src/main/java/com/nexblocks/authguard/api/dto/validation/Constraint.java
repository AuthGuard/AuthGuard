package com.nexblocks.authguard.api.dto.validation;

import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.Optional;

public interface Constraint<T> {
    Optional<Violation> validate(final String fieldName, final T value);
}
