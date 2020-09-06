package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.requests.AccountEmailsRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class AccountEmailsRequestValidator implements Validator<AccountEmailsRequestDTO> {
    @Override
    public List<Violation> validate(final AccountEmailsRequestDTO obj) {
        return FluentValidator.begin()
                .validate("emails", obj.getEmails(), Constraints.required, Constraints.hasItems)
                .validateCollection("emails", obj.getEmails(), Constraints.validEmail)
                .getViolations();
    }
}
