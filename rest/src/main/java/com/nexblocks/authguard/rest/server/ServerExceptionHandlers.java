package com.nexblocks.authguard.rest.server;

import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.rest.exceptions.ExceptionHandlers;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.exceptions.RuntimeJsonException;
import com.nexblocks.authguard.service.exceptions.IdempotencyException;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceConflictException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

public class ServerExceptionHandlers implements JavalinAppConfigurer {
    private final static Logger log = LoggerFactory.getLogger(AuthGuardServer.class.getSimpleName());

    @Override
    public void configure(final Javalin app) {
        app.exception(Exception.class, (e, context) -> {
            log.error("An exception was thrown", e);

            final String message = "An error occurred while processing request " + context.method() + " " + context.path();

            context.status(500)
                    .json(new Error("", message));
        });

        app.exception(ServiceException.class, ExceptionHandlers::serviceException);

        app.exception(ServiceAuthorizationException.class, ExceptionHandlers::serviceAuthorizationException);

        app.exception(ServiceConflictException.class, ExceptionHandlers::serviceConflictException);

        app.exception(RuntimeJsonException.class, ExceptionHandlers::jsonMappingException);

        app.exception(RequestValidationException.class, ExceptionHandlers::requestValidationException);

        app.exception(IdempotencyException.class, ExceptionHandlers::idempotencyException);

        app.exception(TimeoutException.class, ExceptionHandlers::timeoutException);

        app.exception(CompletionException.class, ExceptionHandlers::completionException);
    }
}
