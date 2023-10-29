package com.nexblocks.authguard.api.common;


import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class RequestValidationException extends RuntimeException {
    private final List<Violation> violations;

    public RequestValidationException(final List<Violation> violations) {
        this.violations = violations;
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
