package com.authguard.rest.exceptions;

import com.authguard.api.dto.entities.Error;
import com.authguard.api.dto.entities.RequestValidationError;
import com.authguard.service.exceptions.IdempotencyException;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.IdempotentRecordBO;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;

public class ExceptionHandlers {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlers.class);

    public static void serviceException(final ServiceException e, final Context context) {
        LOG.info("Service exception was thrown");

        final Error error = new Error(e.getErrorCode().getCode(), e.getMessage());
        context.status(400)
                .json(error);
    }

    public static void serviceConflictException(final ServiceConflictException e, final Context context) {
        LOG.info("Service conflict exception was thrown");

        final Error error = new Error(e.getErrorCode().getCode(), e.getMessage());
        context.status(409)
                .json(error);
    }

    public static void serviceAuthorizationException(final ServiceAuthorizationException e, final Context context) {
        LOG.info("Service authorization exception was thrown");

        final Error error = new Error(e.getErrorCode().getCode(), e.getMessage());
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
        } else if (cause instanceof ServiceException) {
            serviceException((ServiceException) cause, context);
        } else if (cause instanceof RuntimeJsonException) {
            jsonMappingException((RuntimeJsonException) cause, context);
        } else if (cause instanceof RequestValidationException) {
            requestValidationException((RequestValidationException) cause, context);
        } else if (cause instanceof IdempotencyException) {
            idempotencyException((IdempotencyException) cause, context);
        } else {
            LOG.error("An unexpected exception was thrown", cause);

            context.status(500).json(new Error("UNKNOWN", "An unknown error occurred"));
        }
    }
}
