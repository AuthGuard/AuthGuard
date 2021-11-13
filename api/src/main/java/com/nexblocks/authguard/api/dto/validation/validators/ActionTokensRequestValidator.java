package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.entities.ActionTokenRequestType;
import com.nexblocks.authguard.api.dto.requests.ActionTokenRequestDTO;
import com.nexblocks.authguard.api.dto.requests.OtpRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;

import java.util.Collections;
import java.util.List;

public class ActionTokensRequestValidator implements Validator<ActionTokenRequestDTO> {
    @Override
    public List<Violation> validate(final ActionTokenRequestDTO obj) {
        return FluentValidator.begin()
                .validate("type", obj.getType(), Constraints.required)
                .validate("action", obj.getAction(), Constraints.required)
                .validate("otp", obj.getOtp(), otp -> {
                    if (obj.getType() != ActionTokenRequestType.OTP) {
                        return Collections.emptyList();
                    }

                    if (otp == null) {
                        return Collections.singletonList(new Violation("otp", ViolationType.MISSING_REQUIRED_VALUE));
                    }

                    return Validators.getForClass(OtpRequestDTO.class).validate(otp);
                })
                .validate("basic", obj.getBasic(), basic -> {
                    if (obj.getType() != ActionTokenRequestType.BASIC) {
                        return Collections.emptyList();
                    }

                    if (basic == null) {
                        return Collections.singletonList(new Violation("basic", ViolationType.MISSING_REQUIRED_VALUE));
                    }

                    if (basic.getIdentifier() == null) {
                        return Collections.singletonList(new Violation("basic.identifier", ViolationType.MISSING_REQUIRED_VALUE));
                    }

                    if (basic.getPassword() == null) {
                        return Collections.singletonList(new Violation("basic.identifier", ViolationType.MISSING_REQUIRED_VALUE));
                    }

                    return Collections.emptyList();
                })
                .getViolations();
    }
}
