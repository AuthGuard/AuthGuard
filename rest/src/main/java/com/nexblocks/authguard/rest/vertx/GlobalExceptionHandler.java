package com.nexblocks.authguard.rest.vertx;

import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.common.RuntimeJsonException;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.rest.exceptions.ExceptionHandlers;
import com.nexblocks.authguard.service.exceptions.*;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

public class GlobalExceptionHandler implements Handler<RoutingContext> {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public void handle(RoutingContext context) {
        Throwable failure = context.failure();

        if (failure == null) {
            context.next(); // no failure
            return;
        }

        if (failure instanceof RequestValidationException e) {
            ExceptionHandlers.requestValidationException(e, context.response());
        } else if (failure instanceof RuntimeJsonException e) {
            ExceptionHandlers.jsonMappingException(e, context.response());
        } else if (failure instanceof ServiceConflictException e) {
            ExceptionHandlers.serviceConflictException(e, context.response());
        } else if (failure instanceof ServiceAuthorizationException e) {
            ExceptionHandlers.serviceAuthorizationException(e, context.response());
        } else if (failure instanceof ServiceNotFoundException e) {
            ExceptionHandlers.serviceNotFoundException(e, context.response());
        } else if (failure instanceof ServiceException e) {
            ExceptionHandlers.serviceException(e, context.response());
        } else if (failure instanceof TimeoutException e) {
            ExceptionHandlers.timeoutException(e, context.response());
        } else if (failure instanceof IdempotencyException e) {
            ExceptionHandlers.idempotencyException(e, context.response());
        } else if (failure instanceof CompletionException e) {
            ExceptionHandlers.completionException(e, context.response());
        } else {
            log.error("Unhandled exception", failure);

            context.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(io.vertx.core.json.Json.encode(
                            new Error("", "An error occurred while processing the request")
                    ));
        }
    }
}
