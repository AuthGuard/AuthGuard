package com.authguard.api.dto.validation.validators;

import com.authguard.api.dto.entities.UserIdentifierDTO;
import com.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.authguard.api.dto.requests.CreateCompleteAccountRequestDTO;
import com.authguard.api.dto.requests.CreateCredentialsRequestDTO;
import com.authguard.api.dto.validation.Validator;
import com.authguard.api.dto.validation.fluent.FluentValidator;
import com.authguard.api.dto.validation.violations.Violation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateCompleteAccountRequestValidator implements Validator<CreateCompleteAccountRequestDTO> {
    private final Validator<CreateAccountRequestDTO> accountRequestValidator;

    public CreateCompleteAccountRequestValidator() {
        accountRequestValidator = Validators.getForClass(CreateAccountRequestDTO.class);
    }

    @Override
    public List<Violation> validate(final CreateCompleteAccountRequestDTO obj) {
        final List<Violation> violations = FluentValidator.begin()
                .validate("account", obj.getAccount(), Constraints.required)
                .validate("credentials", obj.getCredentials(), Constraints.required)
                .getViolations();

        if (violations.isEmpty()) {
            return Stream.concat(accountRequestValidator.validate(obj.getAccount()).stream(),
                    validateCredentials(obj.getCredentials()).stream()
            ).collect(Collectors.toList());
        }

        return violations;
    }

    // we need this here without the accountId
    private List<Violation> validateCredentials(final CreateCredentialsRequestDTO obj) {
        return FluentValidator.begin()
                .validate("identifiers", obj.getIdentifiers(), Constraints.required, Constraints.hasItems)
                .validate("plainPassword", obj.getPlainPassword(), Constraints.required)
                .validateCollection("identifiers", obj.getIdentifiers(), Validators.getForClass(UserIdentifierDTO.class))
                .getViolations();
    }
}
