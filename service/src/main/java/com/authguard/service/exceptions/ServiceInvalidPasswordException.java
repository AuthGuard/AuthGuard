package com.authguard.service.exceptions;

import com.authguard.service.passwords.Violation;

import java.util.List;
import java.util.stream.Collectors;

public class ServiceInvalidPasswordException extends ServiceException {
    public ServiceInvalidPasswordException(final List<Violation> violations) {
        super(violations.stream()
                .map(Violation::getMessage)
                .collect(Collectors.joining(", "))
        );
    }
}
