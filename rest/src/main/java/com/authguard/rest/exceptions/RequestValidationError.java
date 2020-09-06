package com.authguard.rest.exceptions;

import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class RequestValidationError {
    private final List<Violation> violations;

    public RequestValidationError(final List<Violation> violations) {
        this.violations = violations;
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
