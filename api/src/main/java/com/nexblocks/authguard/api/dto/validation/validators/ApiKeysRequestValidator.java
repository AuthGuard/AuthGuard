package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.ApiKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.dto.validation.violations.Violations;

import java.util.Collections;
import java.util.List;

public class ApiKeysRequestValidator implements Validator<ApiKeyRequestDTO> {
    @Override
    public List<Violation> validate(final ApiKeyRequestDTO obj) {
        return FluentValidator.begin()
                .validate("appId", obj.getAppId(), Constraints.required)
                .validate("keyType", obj.getKeyType(), Constraints.required, Constraints.reasonableLength)
                .validate("validFor", obj.getValidFor(), durationRequest -> {
                    if (durationRequest == null) {
                        return Collections.emptyList();
                    }

                    if (durationRequest.getDays() < 0) {
                        return Collections.singletonList(Violations.invalidValue("validFor.days"));
                    }

                    if (durationRequest.getHours() < 0) {
                        return Collections.singletonList(Violations.invalidValue("validFor.hours"));
                    }

                    if (durationRequest.getMinutes() < 0) {
                        return Collections.singletonList(Violations.invalidValue("validFor.minutes"));
                    }

                    return Collections.emptyList();
                })
                .getViolations();
    }
}
