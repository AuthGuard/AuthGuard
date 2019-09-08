package org.auther.service.exceptions;

public class ServiceAuthorizationException extends ServiceException {
    public ServiceAuthorizationException(final String errorCode, final String message) {
        super(errorCode, message);
    }

    public ServiceAuthorizationException(final String message) {
        super(message);
    }
}
