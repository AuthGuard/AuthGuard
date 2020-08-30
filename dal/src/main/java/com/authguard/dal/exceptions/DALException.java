package com.authguard.dal.exceptions;

public class DALException extends RuntimeException {
    public DALException() {
    }

    public DALException(final String message) {
        super(message);
    }
}
