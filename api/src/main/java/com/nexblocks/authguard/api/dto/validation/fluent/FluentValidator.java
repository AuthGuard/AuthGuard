package com.nexblocks.authguard.api.dto.validation.fluent;

import com.nexblocks.authguard.api.dto.validation.Constraint;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FluentValidator {
    private final List<Violation> violations;

    private FluentValidator() {
        violations = new ArrayList<>();
    }

    public static FluentValidator begin() {
        return new FluentValidator();
    }

    @SafeVarargs
    public final <V> FluentValidator validate(final String fieldName, final V value,
                                              final Constraint<V>... validators) {
        for (Constraint<V> validator : validators) {
            validator.validate(fieldName, value).ifPresent(violations::add);
        }

        return this;
    }

    public <V> FluentValidator validate(final String fieldName, final V value,
                                        final Validator<V> validator) {
        violations.addAll(validator.validate(value, fieldName));

        return this;
    }

    public <V> FluentValidator validateRequired(final String fieldName, final V value,
                                                final Validator<V> validator) {
        if (value == null) {
            violations.add(new Violation(fieldName, ViolationType.MISSING_REQUIRED_VALUE));
        } else {
            violations.addAll(validator.validate(value, fieldName));
        }

        return this;
    }

    @SafeVarargs
    public final <V> FluentValidator validateCollection(final String fieldName, final Collection<V> values,
                                                        final Constraint<V>... validators) {
        values.forEach(value -> {
            for (Constraint<V> validator : validators) {
                validator.validate(fieldName, value).ifPresent(violations::add);
            }
        });

        return this;
    }

    @SafeVarargs
    public final <V> FluentValidator validateCollection(final String fieldName, final Collection<V> values,
                                                        final Validator<V>... validators) {
        values.forEach(value -> {
            for (Validator<V> validator : validators) {
                violations.addAll(validator.validate(value, fieldName));
            }
        });

        return this;
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
