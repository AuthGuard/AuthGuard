package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.UserIdentifierDTO;
import com.authguard.api.dto.requests.CreateCredentialsRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CreateCredentialsRequestValidator implements Validator<CreateCredentialsRequestDTO> {

    @Override
    public List<Violation> validate(final CreateCredentialsRequestDTO obj) {
        return FluentValidator.begin()
                .validate("accountId", obj.getAccountId(), Constraints.required, Constraints.reasonableLength)
                .validate("identifiers", obj.getIdentifiers(), Constraints.required, Constraints.hasItems)
                .validate("plainPassword", obj.getPlainPassword(), Constraints.required)
                .validateCollection("identifiers", obj.getIdentifiers(), Validators.getForClass(UserIdentifierDTO.class))
                .getViolations();
    }
}
