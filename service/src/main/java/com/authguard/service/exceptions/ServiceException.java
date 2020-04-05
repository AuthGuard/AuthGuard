package com.authguard.service.exceptions;

public class ServiceException extends RuntimeException {
    private final String errorCode;

    public ServiceException(final String errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ServiceException(final String message) {
        this(null, message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
