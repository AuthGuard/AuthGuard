package com.nexblocks.authguard.rest.exceptions;

import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.common.RuntimeJsonException;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.service.exceptions.*;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.IdempotentRecordBO;
import io.javalin.http.Context;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

public class ExceptionHandlers {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlers.class);

    public static void serviceException(final ServiceException e, final Context context) {
        LOG.debug("Service exception was thrown", e);

        final Error error = new Error(e.getErrorCode(), e.getMessage());
        context.status(400)
                .json(error);
    }

    public static void serviceNotFoundException(final ServiceNotFoundException e, final Context context) {
        LOG.debug("Service not found exception was thrown", e);

        final Error error = new Error(e.getErrorCode(), e.getMessage());
        context.status(404)
                .json(error);
    }


    public static void serviceConflictException(final ServiceConflictException e, final Context context) {
        LOG.debug("Service conflict exception was thrown", e);

        final Error error = new Error(e.getErrorCode(), e.getMessage());
        context.status(409)
                .json(error);
    }

    public static void serviceAuthorizationException(final ServiceAuthorizationException e, final Context context) {
        LOG.debug("Service authorization exception was thrown", e);

        final Error error = new Error(e.getErrorCode(), e.getMessage());
        /*
         * We leave 401 to signal an unauthorized access.
         * TODO: don't rely on exceptions to highlight something like that and instead
         *  make the service return the error using an Either.
         */
        context.status(400).json(error);
    }

    public static void jsonMappingException(final RuntimeJsonException e, final Context context) {
        final Error error = new Error("", "Failed to parse JSON at " + e.getCause().getLocation());
        context.status(422).json(error);
    }

    public static void requestValidationException(final RequestValidationException e, final Context context) {
        final RequestValidationError error = new RequestValidationError(e.getViolations());
        context.status(400).json(error);
    }

    public static void idempotencyException(final IdempotencyException e, final Context context) {
        final IdempotentRecordBO record = e.getIdempotentRecord();

        final Error error = new Error(ErrorCode.IDEMPOTENCY_ERROR.getCode(),
                String.format("Idempotent key %s was already used to create entity %s of type %s", record.getIdempotentKey(),
                        record.getEntityId(), record.getEntityType()));

        context.status(409).json(error);
    }

    public static void timeoutException(final TimeoutException e, final Context context) {
        final Error error = new Error("504", "Timeout");

        LOG.warn("A timeout error occurred", e);

        context.status(504).json(error);
    }

    public static void serviceException(final ServiceException e, final HttpServerResponse response) {
        LOG.debug("Service exception was thrown", e);

        final Error error = new Error(e.getErrorCode(), e.getMessage());
        respondWith(response, 400, error);
    }

    public static void serviceNotFoundException(final ServiceNotFoundException e, final HttpServerResponse response) {
        LOG.debug("Service not found exception was thrown", e);

        final Error error = new Error(e.getErrorCode(), e.getMessage());
        respondWith(response, 404, error);
    }


    public static void serviceConflictException(final ServiceConflictException e, final HttpServerResponse response) {
        LOG.debug("Service conflict exception was thrown", e);

        final Error error = new Error(e.getErrorCode(), e.getMessage());
        respondWith(response, 409, error);
    }

    public static void serviceAuthorizationException(final ServiceAuthorizationException e, final HttpServerResponse response) {
        LOG.debug("Service authorization exception was thrown", e);

        final Error error = new Error(e.getErrorCode(), e.getMessage());
        /*
         * We leave 401 to signal an unauthorized access.
         * TODO: don't rely on exceptions to highlight something like that and instead
         *  make the service return the error using an Either.
         */
        respondWith(response, 400, error);
    }

    public static void jsonMappingException(final RuntimeJsonException e, final HttpServerResponse response) {
        final Error error = new Error("", "Failed to parse JSON at " + e.getCause().getLocation());
        respondWith(response, 422, error);
    }

    public static void requestValidationException(final RequestValidationException e, final HttpServerResponse response) {
        final RequestValidationError error = new RequestValidationError(e.getViolations());
        respondWith(response, 400, error);
    }

    public static void idempotencyException(final IdempotencyException e, final HttpServerResponse response) {
        final IdempotentRecordBO record = e.getIdempotentRecord();

        final Error error = new Error(ErrorCode.IDEMPOTENCY_ERROR.getCode(),
                String.format("Idempotent key %s was already used to create entity %s of type %s", record.getIdempotentKey(),
                        record.getEntityId(), record.getEntityType()));

        respondWith(response, 409, error);
    }

    public static void timeoutException(final TimeoutException e, final HttpServerResponse response) {
        final Error error = new Error("504", "Timeout");

        LOG.warn("A timeout error occurred", e);

        respondWith(response, 504, error);
    }

    private static void respondWith(HttpServerResponse response, int statusCode, Error error) {
        response.setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(error));
    }

    private static void respondWith(HttpServerResponse response, int statusCode, RequestValidationError error) {
        response.setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(error));
    }

    // NOTE: this will go away when we move to async services
    public static void completionException(final CompletionException e, final Context context) {
        final Throwable cause = e.getCause();

        if (cause == null) {
            LOG.error("A CompletionException was thrown without a cause", e);

            context.status(500).json(new Error("UNKNOWN", "An unknown error occurred"));
        } else if (cause instanceof ServiceAuthorizationException) {
            serviceAuthorizationException((ServiceAuthorizationException) cause, context);
        } else if (cause instanceof ServiceConflictException) {
            serviceConflictException((ServiceConflictException) cause, context);
        } else if (cause instanceof ServiceNotFoundException) {
            serviceNotFoundException((ServiceNotFoundException) cause, context);
        } else if (cause instanceof ServiceException) {
            serviceException((ServiceException) cause, context);
        } else if (cause instanceof RuntimeJsonException) {
            jsonMappingException((RuntimeJsonException) cause, context);
        } else if (cause instanceof RequestValidationException) {
            requestValidationException((RequestValidationException) cause, context);
        } else if (cause instanceof IdempotencyException) {
            idempotencyException((IdempotencyException) cause, context);
        } else if (cause instanceof TimeoutException) {
            timeoutException((TimeoutException) cause, context);
        } else {
            LOG.error("An unexpected exception was thrown", cause);

            context.status(500).json(new Error("UNKNOWN", "An unknown error occurred"));
        }
    }

    public static void completionException(final CompletionException e, HttpServerResponse context) {
        final Throwable cause = e.getCause();

        if (cause == null) {
            LOG.error("A CompletionException was thrown without a cause", e);

            context.setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(io.vertx.core.json.Json.encode(
                            new Error("", "An error occurred while processing the request")
                    ));
        } else if (cause instanceof ServiceAuthorizationException) {
            serviceAuthorizationException((ServiceAuthorizationException) cause, context);
        } else if (cause instanceof ServiceConflictException) {
            serviceConflictException((ServiceConflictException) cause, context);
        } else if (cause instanceof ServiceNotFoundException) {
            serviceNotFoundException((ServiceNotFoundException) cause, context);
        } else if (cause instanceof ServiceException) {
            serviceException((ServiceException) cause, context);
        } else if (cause instanceof RuntimeJsonException) {
            jsonMappingException((RuntimeJsonException) cause, context);
        } else if (cause instanceof RequestValidationException) {
            requestValidationException((RequestValidationException) cause, context);
        } else if (cause instanceof IdempotencyException) {
            idempotencyException((IdempotencyException) cause, context);
        } else if (cause instanceof TimeoutException) {
            timeoutException((TimeoutException) cause, context);
        } else {
            LOG.error("An unexpected exception was thrown", cause);

            context.setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(io.vertx.core.json.Json.encode(
                            new Error("", "An error occurred while processing the request")
                    ));
        }
    }
}
