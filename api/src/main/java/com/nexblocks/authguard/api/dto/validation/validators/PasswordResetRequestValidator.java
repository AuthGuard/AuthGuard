package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.PasswordResetRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class PasswordResetRequestValidator implements Validator<PasswordResetRequestDTO> {
    @Override
    public List<Violation> validate(final PasswordResetRequestDTO obj) {
        return FluentValidator.begin()
                .validate("resetToken", obj.getResetToken(), Constraints.required)
                .validate("plainPassword", obj.getPlainPassword(), Constraints.required)
                .getViolations();
    }
}
