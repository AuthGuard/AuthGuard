package org.auther.service.exceptions;

public class ServiceNotFoundException extends ServiceException {
    public ServiceNotFoundException(final String errorCode, final String message) {
        super(errorCode, message);
    }

    public ServiceNotFoundException(final String message) {
        super(message);
    }


    public ServiceNotFoundException() {
        super(null, "");
    }
}
