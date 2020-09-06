package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.UserIdentifierDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class UserIdentifierValidator implements Validator<UserIdentifierDTO> {
    @Override
    public List<Violation> validate(final UserIdentifierDTO obj) {
        return FluentValidator.begin()
                .validate("type", obj.getType(), Constraints.required)
                .validate("identifier", obj.getIdentifier(), Constraints.required, Constraints.reasonableLength)
                .getViolations();
    }
}
