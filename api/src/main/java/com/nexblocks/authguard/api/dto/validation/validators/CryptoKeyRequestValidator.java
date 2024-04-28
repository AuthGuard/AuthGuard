package com.nexblocks.authguard.api.dto.validation.validators;

import com.nexblocks.authguard.api.dto.requests.CryptoKeyRequestDTO;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.fluent.FluentValidator;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

public class CryptoKeyRequestValidator implements Validator<CryptoKeyRequestDTO> {
    @Override
    public List<Violation> validate(final CryptoKeyRequestDTO obj) {
        return FluentValidator.begin()
                .validate("algorithm", obj.getAlgorithm(), Constraints.required)
                .validate("size", obj.getSize(), Constraints.required)
                .getViolations();
    }
}
