package com.authguard.dal.exceptions;

public class IllegalOperationException extends DALException {
    public IllegalOperationException() {
    }

    public IllegalOperationException(final String message) {
        super(message);
    }
}
