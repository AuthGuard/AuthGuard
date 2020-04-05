package com.authguard.rest.exceptions;

import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.exceptions.ServiceException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.javalin.http.ExceptionHandler;

public class ExceptionHandlers {
    public static ExceptionHandler<ServiceException> serviceException() {
        return (e, context) -> {
            final Error error = new Error(e.getErrorCode(), e.getMessage());
            context.status(400)
                    .json(error);
        };
    }

    public static ExceptionHandler<ServiceConflictException> serviceConflictException() {
        return (e, context) -> {
            final Error error = new Error(e.getErrorCode(), e.getMessage());
            context.status(409)
                    .json(error);
        };
    }

    public static ExceptionHandler<ServiceAuthorizationException> serviceAuthorizationException() {
        return (e, context) -> {
            final Error error = new Error(e.getErrorCode(), e.getMessage());
            context.status(200)
                    .json(error);
        };
    }

    public static ExceptionHandler<RuntimeJsonException> jsonMappingException() {
        return (e, context) -> {
            final Error error = new Error("", "Failed to parse JSON at " + e.getCause().getLocation());
            context.status(422)
                    .json(error);
        };
    }
}
