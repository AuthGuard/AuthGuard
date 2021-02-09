package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.PasswordlessRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class PasswordlessRequestValidator implements Validator<PasswordlessRequestDTO> {
    @Override
    public List<Violation> validate(final PasswordlessRequestDTO obj) {
        return FluentValidator.begin()
                .validate("token", obj.getToken(), Constraints.required)
                .getViolations();
    }
}
