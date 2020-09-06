package com.authguard.api.dto.validation.basic;

import com.authguard.api.dto.entities.AccountEmailDTO;
import com.authguard.api.dto.validation.Constraint;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.api.dto.validation.violations.ViolationType;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.Optional;

public class ValidEmail implements Constraint<AccountEmailDTO> {
    private final EmailValidator emailValidator;

    public ValidEmail() {
        emailValidator = EmailValidator.getInstance();
    }

    @Override
    public Optional<Violation> validate(final String fieldName, final AccountEmailDTO obj) {
        if (!emailValidator.isValid(obj.getEmail())) {
            final Violation violation = new Violation("email", ViolationType.INVALID_VALUE);

            return Optional.of(violation);
        }

        return Optional.empty();
    }
}
