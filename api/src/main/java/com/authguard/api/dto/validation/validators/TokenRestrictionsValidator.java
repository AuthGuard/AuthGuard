package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.TokenRestrictionsDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class TokenRestrictionsValidator implements Validator<TokenRestrictionsDTO> {
    @Override
    public List<Violation> validate(final TokenRestrictionsDTO obj) {
        return FluentValidator.begin()
                .validateCollection("permissions", obj.getPermissions(), Constraints.reasonableLength)
                .validateCollection("scopes", obj.getScopes(), Constraints.reasonableLength)
                .getViolations();
    }
}
