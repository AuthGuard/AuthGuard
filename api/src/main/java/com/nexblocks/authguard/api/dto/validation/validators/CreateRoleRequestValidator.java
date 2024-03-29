package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CreateRoleRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CreateRoleRequestValidator implements Validator<CreateRoleRequestDTO> {
    @Override
    public List<Violation> validate(final CreateRoleRequestDTO obj) {
        return FluentValidator.begin()
                .validate("name", obj.getName(), Constraints.required, Constraints.reasonableLength)
                .validate("domain", obj.getDomain(), Constraints.required)
                .validate("forAccounts", obj.isForAccounts(), Constraints.required)
                .validate("forApplications", obj.isForApplications(), Constraints.required)
                .getViolations();
    }
}
