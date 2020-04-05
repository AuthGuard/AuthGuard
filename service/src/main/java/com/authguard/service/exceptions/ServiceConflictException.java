package com.authguard.service.exceptions;

public class ServiceConflictException extends ServiceException {
    public ServiceConflictException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ServiceConflictException(String message) {
        super(message);
    }
}
