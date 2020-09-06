package com.authguard.rest.server;

import com.authguard.rest.exceptions.Error;
import com.authguard.rest.exceptions.ExceptionHandlers;
import com.authguard.rest.exceptions.RequestValidationException;
import com.authguard.rest.exceptions.RuntimeJsonException;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.exceptions.ServiceException;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerExceptionHandlers implements ServerConfigurer {
    private final static Logger log = LoggerFactory.getLogger(Server.class.getSimpleName());

    @Override
    public void configureFor(final Javalin app) {
        app.exception(Exception.class, (e, context) -> {
            log.error("An exception was thrown", e);

            final String message = "An error occurred while processing request " + context.method() + " " + context.path();

            context.status(500)
                    .json(new Error("", message));
        });

        app.exception(ServiceException.class, ExceptionHandlers.serviceException());

        app.exception(ServiceAuthorizationException.class, ExceptionHandlers.serviceAuthorizationException());

        app.exception(ServiceConflictException.class, ExceptionHandlers.serviceConflictException());

        app.exception(RuntimeJsonException.class, ExceptionHandlers.jsonMappingException());

        app.exception(RequestValidationException.class, ExceptionHandlers.requestValidationException());
    }
}
