package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.ApiKeyVerificationRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class ApiKeyVerificationRequestValidator implements Validator<ApiKeyVerificationRequestDTO> {
    @Override
    public List<Violation> validate(final ApiKeyVerificationRequestDTO obj) {
        return FluentValidator.begin()
                .validate("key", obj.getKey(), Constraints.required, Constraints.reasonableLength)
                .validate("keyType", obj.getKeyType(), Constraints.required, Constraints.reasonableLength)
                .getViolations();
    }
}
