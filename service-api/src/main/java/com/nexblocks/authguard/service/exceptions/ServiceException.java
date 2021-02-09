package com.nexblocks.authguard.service.exceptions;

import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;

public class ServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public ServiceException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
