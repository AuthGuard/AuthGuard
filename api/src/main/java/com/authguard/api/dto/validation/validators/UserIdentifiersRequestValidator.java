package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.UserIdentifierDTO;
import com.authguard.api.dto.requests.UserIdentifiersRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class UserIdentifiersRequestValidator implements Validator<UserIdentifiersRequestDTO> {
    @Override
    public List<Violation> validate(final UserIdentifiersRequestDTO obj) {
        return FluentValidator.begin()
                .validate("identifiers", obj.getIdentifiers(), Constraints.required, Constraints.hasItems)
                .validateCollection("identifiers", obj.getIdentifiers(), Validators.getForClass(UserIdentifierDTO.class))
                .getViolations();
    }
}
