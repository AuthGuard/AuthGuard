package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.requests.OtpRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class OtpRequestValidator implements Validator<OtpRequestDTO> {
    @Override
    public List<Violation> validate(final OtpRequestDTO obj) {
        return FluentValidator.begin()
                .validate("passwordId", obj.getPasswordId(), Constraints.required)
                .validate("password", obj.getPassword(), Constraints.required)
                .getViolations();
    }
}
