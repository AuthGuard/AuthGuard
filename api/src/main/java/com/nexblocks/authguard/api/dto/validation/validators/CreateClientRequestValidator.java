package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CreateClientRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CreateClientRequestValidator implements Validator<CreateClientRequestDTO> {
    @Override
    public List<Violation> validate(CreateClientRequestDTO obj) {
        return FluentValidator.begin()
                .validate("externalId", obj.getExternalId(), Constraints.reasonableLength)
                .validate("accountId", obj.getAccountId(), Constraints.reasonableLength)
                .validate("name", obj.getName(), Constraints.required, Constraints.reasonableLength)
                .validate("domain", obj.getDomain(), Constraints.required)
                .validate("clientType", obj.getClientType(), Constraints.required)
                .getViolations();
    }
}
