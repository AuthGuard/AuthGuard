package com.authguard.rest.exceptions;

import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.exceptions.ServiceException;
import io.javalin.http.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandlers {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlers.class);

    public static ExceptionHandler<ServiceException> serviceException() {
        return (e, context) -> {
            LOG.info("Service exception was thrown");

            final Error error = new Error(e.getErrorCode(), e.getMessage());
            context.status(400)
                    .json(error);
        };
    }

    public static ExceptionHandler<ServiceConflictException> serviceConflictException() {
        return (e, context) -> {
            LOG.info("Service conflict exception was thrown");

            final Error error = new Error(e.getErrorCode(), e.getMessage());
            context.status(409)
                    .json(error);
        };
    }

    public static ExceptionHandler<ServiceAuthorizationException> serviceAuthorizationException() {
        return (e, context) -> {
            LOG.info("Service authorization exception was thrown");

            final Error error = new Error(e.getErrorCode(), e.getMessage());
            /*
             * We leave 401 to signal an unauthorized access.
             * TODO: don't rely on exceptions to highlight something like that and instead
             *  make the service return the error using an Either.
             */
            context.status(400).json(error);
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
