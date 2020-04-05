package com.authguard.rest.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;

public class RuntimeJsonException extends RuntimeException {
    public RuntimeJsonException(JsonProcessingException cause) {
        super(cause);
    }
}
