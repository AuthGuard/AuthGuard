package com.nexblocks.authguard.api.dto.validation.basic;

import com.nexblocks.authguard.api.dto.validation.Constraint;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;

import java.util.Collection;
import java.util.Optional;

public class HasItems implements Constraint<Collection> {
    @Override
    public Optional<Violation> validate(final String fieldName, final Collection value) {
        return value.isEmpty() ? Optional.of(new Violation(fieldName, ViolationType.EMPTY_LIST))
                : Optional.empty();
    }
}
