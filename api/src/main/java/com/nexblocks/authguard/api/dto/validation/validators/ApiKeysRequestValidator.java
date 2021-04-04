package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.ApiKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class ApiKeysRequestValidator implements Validator<ApiKeyRequestDTO> {
    @Override
    public List<Violation> validate(final ApiKeyRequestDTO obj) {
        return FluentValidator.begin()
                .validate("appId", obj.getAppId(), Constraints.required, Constraints.reasonableLength)
                .getViolations();
    }
}
