package com.nexblocks.authguard.service.exceptions;

import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;

public class ServiceNotFoundException extends ServiceException {
    public ServiceNotFoundException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }
}
