package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.requests.CreateRoleRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CreateRoleRequestValidator implements Validator<CreateRoleRequestDTO> {
    @Override
    public List<Violation> validate(final CreateRoleRequestDTO obj) {
        return FluentValidator.begin()
                .validate("name", obj.getName(), Constraints.required, Constraints.reasonableLength)
                .getViolations();
    }
}
