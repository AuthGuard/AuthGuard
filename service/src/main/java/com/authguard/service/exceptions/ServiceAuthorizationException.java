package com.authguard.service.exceptions;

import com.authguard.service.exceptions.codes.ErrorCode;

public class ServiceAuthorizationException extends ServiceException {
    public ServiceAuthorizationException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
}
