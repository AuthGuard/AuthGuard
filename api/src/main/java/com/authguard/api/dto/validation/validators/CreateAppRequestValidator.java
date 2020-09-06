package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.requests.CreateAppRequest;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CreateAppRequestValidator implements Validator<CreateAppRequest> {
    @Override
    public List<Violation> validate(final CreateAppRequest obj) {
        return FluentValidator.begin()
                .validate("externalId", obj.getExternalId(), Constraints.reasonableLength)
                .validate("accountId", obj.getAccountId(), Constraints.reasonableLength)
                .validate("name", obj.getName(), Constraints.required, Constraints.reasonableLength)
                .getViolations();
    }
}
