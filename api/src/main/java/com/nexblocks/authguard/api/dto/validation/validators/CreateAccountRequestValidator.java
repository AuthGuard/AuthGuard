package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CreateAccountRequestValidator implements Validator<CreateAccountRequestDTO> {

    @Override
    public List<Violation> validate(final CreateAccountRequestDTO obj) {
        return FluentValidator.begin()
                .validate("externalId", obj.getExternalId(), Constraints.reasonableLength)
                .validate("email", obj.getEmail(), Constraints.validEmail)
                .validate("backupEmail", obj.getBackupEmail(), Constraints.validEmail)
                .getViolations();
    }
}
