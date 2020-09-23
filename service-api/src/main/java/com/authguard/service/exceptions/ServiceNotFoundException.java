package com.authguard.service.exceptions;

import com.authguard.service.exceptions.codes.ErrorCode;

public class ServiceNotFoundException extends ServiceException {
    public ServiceNotFoundException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
}
