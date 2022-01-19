package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.TokenRestrictionsDTO;
import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.Collections;
import java.util.List;

public class AuthRequestValidator implements Validator<AuthRequestDTO> {
    @Override
    public List<Violation> validate(final AuthRequestDTO obj) {
        return FluentValidator.begin()
                .validate("restrictions", obj.getRestrictions(), restrictions -> {
                    if (restrictions != null) {
                        return Validators.getForClass(TokenRestrictionsDTO.class).validate(restrictions);
                    }

                    return Collections.emptyList();
                })
                .getViolations();
    }
}
