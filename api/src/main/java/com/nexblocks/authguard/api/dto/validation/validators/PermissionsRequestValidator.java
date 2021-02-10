package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.requests.PermissionsRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class PermissionsRequestValidator implements Validator<PermissionsRequestDTO> {
    @Override
    public List<Violation> validate(final PermissionsRequestDTO obj) {
        return FluentValidator.begin()
                .validate("permissions", obj.getPermissions(), Constraints.required, Constraints.hasItems)
                .validate("action", obj.getAction(), Constraints.required)
                .validateCollection("permissions", obj.getPermissions(), Validators.getForClass(PermissionDTO.class))
                .getViolations();
    }
}
