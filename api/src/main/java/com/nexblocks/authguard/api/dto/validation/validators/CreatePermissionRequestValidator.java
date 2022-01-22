package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CreatePermissionRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CreatePermissionRequestValidator implements Validator<CreatePermissionRequestDTO> {
    @Override
    public List<Violation> validate(final CreatePermissionRequestDTO obj) {
        return FluentValidator.begin()
                .validate("group", obj.getGroup(), Constraints.required, Constraints.reasonableLength)
                .validate("name", obj.getName(), Constraints.required, Constraints.reasonableLength)
                .validate("domain", obj.getDomain(), Constraints.required)
                .getViolations();
    }
}
