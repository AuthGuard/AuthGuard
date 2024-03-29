package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.UserIdentifierDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;

import java.util.Collections;
import java.util.List;

public class CreateAccountRequestValidator implements Validator<CreateAccountRequestDTO> {

    @Override
    public List<Violation> validate(final CreateAccountRequestDTO obj) {
        return FluentValidator.begin()
                .validate("externalId", obj.getExternalId(), Constraints.reasonableLength)
                .validate("email", obj.getEmail(), Constraints.validEmail)
                .validate("backupEmail", obj.getBackupEmail(), Constraints.validEmail)
                .validate("domain", obj.getDomain(), Constraints.required)
                .validate("identifiers", obj.getIdentifiers(), identifiers -> {
                    if (identifiers == null || identifiers.isEmpty()) {
                        if (obj.getEmail() == null && obj.getPhoneNumber() == null) {
                            return Collections.singletonList(
                                    new Violation("identifiers", ViolationType.MISSING_REQUIRED_VALUE)
                            );
                        }
                    }

                    return Collections.emptyList();
                })
                .validateCollection("identifiers", obj.getIdentifiers(), Validators.getForClass(UserIdentifierDTO.class))
                .getViolations();
    }
}
