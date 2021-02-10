package com.nexblocks.authguard.service.exceptions;

import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;

public class ServiceConflictException extends ServiceException {
    public ServiceConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
