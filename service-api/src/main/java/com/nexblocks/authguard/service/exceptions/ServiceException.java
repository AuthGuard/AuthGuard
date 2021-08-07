package com.nexblocks.authguard.service.exceptions;

import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;

public class ServiceException extends RuntimeException {
    private final String errorCode;

    public ServiceException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode.getCode();
    }

    public ServiceException(final String errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
