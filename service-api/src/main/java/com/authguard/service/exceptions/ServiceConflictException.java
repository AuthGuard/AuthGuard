package com.authguard.service.exceptions;

import com.authguard.service.exceptions.codes.ErrorCode;

public class ServiceConflictException extends ServiceException {
    public ServiceConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
