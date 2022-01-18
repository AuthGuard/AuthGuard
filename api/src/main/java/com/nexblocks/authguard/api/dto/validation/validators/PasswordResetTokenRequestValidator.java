package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.PasswordResetTokenRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class PasswordResetTokenRequestValidator implements Validator<PasswordResetTokenRequestDTO> {
    @Override
    public List<Violation> validate(final PasswordResetTokenRequestDTO obj) {
        return FluentValidator.begin()
                .validate("identifier", obj.getIdentifier(), Constraints.required)
                .getViolations();
    }
}
