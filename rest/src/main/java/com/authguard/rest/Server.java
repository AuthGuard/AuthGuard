package com.authguard.rest;

import com.authguard.config.ConfigContext;
import com.authguard.rest.access.AuthorizationHandler;
import com.authguard.rest.exceptions.Error;
import com.authguard.rest.exceptions.ExceptionHandlers;
import com.authguard.rest.exceptions.RuntimeJsonException;
import com.authguard.rest.routes.*;
import com.authguard.service.exceptions.ServiceConflictException;
import com.google.inject.Injector;
import io.javalin.Javalin;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.path;

class Server {
    private final static Logger log = LoggerFactory.getLogger(Server.class.getSimpleName());

    private final Injector injector;
    private final ConfigContext configContext;

    Server(final Injector injector, final ConfigContext configContext) {
        this.injector = injector;
        this.configContext = configContext;
    }

    void start(final Javalin app, final int port) {
        app.before(context -> context.attribute("time", System.currentTimeMillis()));
        app.before(injector.getInstance(AuthorizationHandler.class));

        app.after(context -> {
            final Long now = System.currentTimeMillis();
            final Long start = context.attribute("time");

            if (start == null) {
                log.info("{} {} - {}", context.method(), context.path(), context.status());
            } else {
                log.info("{} {} - {} {} ms", context.method(), context.path(), context.status(), now - start);
            }
        });

        app.routes(() -> {
            path("/credentials", injector.getInstance(CredentialsRoute.class));
            path("/auth", injector.getInstance(AuthRoute.class));
            path("/keys", injector.getInstance(ApiKeysRoute.class));
            path("/accounts", injector.getInstance(AccountsRoute.class));
            path("/apps", injector.getInstance(ApplicationsRoute.class));
            path("/admin", injector.getInstance(AdminRoute.class));

            if (configContext.get("otp") != null) {
                path("/otp", injector.getInstance(OtpRoute.class));
            }

            if (configContext.get("verification") != null) {
                path("/verification", injector.getInstance(VerificationRoute.class));
            }
        });

        // if we failed to process a request body
        app.exception(RuntimeJsonException.class, ExceptionHandlers.jsonMappingException());

        app.exception(ServiceException.class, ExceptionHandlers.serviceException());
        app.exception(ServiceAuthorizationException.class, ExceptionHandlers.serviceAuthorizationException());
        app.exception(ServiceConflictException.class, ExceptionHandlers.serviceConflictException());

        app.exception(Exception.class, (e, context) -> {
            log.error("An exception was thrown", e);

            final String message = "An error occurred while processing request " + context.method() + " " + context.path();

            context.status(500)
                    .json(new Error("", message));
        });

        // run
        app.start(port);
    }
}
