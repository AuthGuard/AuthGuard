package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.PasswordResetRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;

import java.util.Collections;
import java.util.List;

public class PasswordResetRequestValidator implements Validator<PasswordResetRequestDTO> {
    @Override
    public List<Violation> validate(final PasswordResetRequestDTO obj) {
        return FluentValidator.begin()
                .validate("identifier", obj.getIdentifier(), identifier -> {
                    if (!obj.isByToken() && identifier == null) {
                        return Collections.singletonList(
                                new Violation("identifier", ViolationType.MISSING_REQUIRED_VALUE)
                        );
                    }

                    if (identifier != null && obj.getDomain() == null) {
                        return Collections.singletonList(
                                new Violation("domain", ViolationType.MISSING_REQUIRED_VALUE)
                        );
                    }

                    return Collections.emptyList();
                })
                .validate("oldPassword", obj.getOldPassword(), password -> {
                    if (!obj.isByToken() && password == null) {
                        return Collections.singletonList(
                                new Violation("oldPassword", ViolationType.MISSING_REQUIRED_VALUE)
                        );
                    }

                    return Collections.emptyList();
                })
                .validate("resetToken", obj.getResetToken(), resetToken -> {
                    if (obj.isByToken() && resetToken == null) {
                        return Collections.singletonList(
                                new Violation("resetToken", ViolationType.MISSING_REQUIRED_VALUE)
                        );
                    }

                    return Collections.emptyList();
                })
                .validate("newPassword", obj.getNewPassword(), Constraints.required)
                .getViolations();
    }
}
