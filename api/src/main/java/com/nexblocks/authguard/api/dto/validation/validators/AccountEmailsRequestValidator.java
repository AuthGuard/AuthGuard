package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.AccountEmailsRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class AccountEmailsRequestValidator implements Validator<AccountEmailsRequestDTO> {
    @Override
    public List<Violation> validate(final AccountEmailsRequestDTO obj) {
        return FluentValidator.begin()
                .validate("email", obj.getEmail(), Constraints.validEmail)
                .getViolations();
    }
}
