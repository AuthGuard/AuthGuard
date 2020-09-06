package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.TokenRestrictionsDTO;
import com.authguard.api.dto.requests.AuthRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class AuthRequestValidator implements Validator<AuthRequestDTO> {
    @Override
    public List<Violation> validate(final AuthRequestDTO obj) {
        return FluentValidator.begin()
                .validate("authorization", obj.getAuthorization(), Constraints.required)
                .validate("restrictions", obj.getRestrictions(), Validators.getForClass(TokenRestrictionsDTO.class))
                .getViolations();
    }
}
