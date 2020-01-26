package com.authguard.service.exceptions;

public class ServiceException extends RuntimeException {
    private final String errorCode;
    private final String message;

    public ServiceException(final String errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }

    public ServiceException(final String message) {
        this(null, message);
    }
}
