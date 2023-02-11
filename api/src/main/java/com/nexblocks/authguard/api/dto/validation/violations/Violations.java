package com.nexblocks.authguard.api.dto.validation.violations;

public class Violations {
    public static Violation invalidValue(final String field) {
        return new Violation(field, ViolationType.INVALID_VALUE);
    }
}
