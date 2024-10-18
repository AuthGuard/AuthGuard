package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.TotpKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class TotpKeyRequestValidator implements Validator<TotpKeyRequestDTO> {
    @Override
    public List<Violation> validate(final TotpKeyRequestDTO obj) {
        return FluentValidator.begin()
                .validate("accountId", obj.getAccountId(), Constraints.required)
                .validate("authenticator", obj.getAuthenticator(), Constraints.reasonableLength)
                .getViolations();
    }
}
