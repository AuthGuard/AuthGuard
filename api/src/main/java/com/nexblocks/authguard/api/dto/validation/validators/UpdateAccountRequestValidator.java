package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.UpdateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class UpdateAccountRequestValidator implements Validator<UpdateAccountRequestDTO> {
    @Override
    public List<Violation> validate(final UpdateAccountRequestDTO obj) {
        return FluentValidator.begin()
                .validate("email", obj.getEmail(), Constraints.validEmail)
                .validate("backupEmail", obj.getBackupEmail(), Constraints.validEmail)
                .getViolations();
    }
}
