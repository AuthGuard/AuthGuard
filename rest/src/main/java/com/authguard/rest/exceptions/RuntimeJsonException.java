package com.authguard.rest.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;

public class RuntimeJsonException extends RuntimeException {
    private final JsonProcessingException cause;

    public RuntimeJsonException(JsonProcessingException cause) {
        this.cause = cause;
    }

    @Override
    public JsonProcessingException getCause() {
        return cause;
    }
}
