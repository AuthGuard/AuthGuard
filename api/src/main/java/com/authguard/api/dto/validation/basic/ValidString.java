package com.authguard.api.dto.validation.basic;

import com.authguard.api.dto.validation.Constraint;
import com.authguard.api.dto.validation.violations.Restrictions;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.api.dto.validation.violations.ViolationType;

import java.util.Optional;

public class ValidString implements Constraint<String> {

    @Override
    public Optional<Violation> validate(final String obj, final String withFieldName) {
        if (obj != null) {
            if (obj.length() > Restrictions.MAX_BASIC_STRING_LENGTH) {
                final Violation violation = new Violation(withFieldName, ViolationType.EXCEEDS_LENGTH_BOUNDARIES);
                return Optional.of(violation);
            }
        }

        return Optional.empty();
    }
}
