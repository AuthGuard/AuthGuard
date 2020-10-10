package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CreateAccountRequestValidator implements Validator<CreateAccountRequestDTO> {

    @Override
    public List<Violation> validate(final CreateAccountRequestDTO obj) {
        return FluentValidator.begin()
                .validate("externalId", obj.getExternalId(), Constraints.reasonableLength)
                .validateCollection("emails", obj.getEmails(), Constraints.validEmail)
                .getViolations();
    }
}