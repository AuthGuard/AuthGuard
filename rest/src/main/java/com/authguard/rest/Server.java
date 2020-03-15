package com.authguard.rest;

import com.authguard.rest.access.AuthorizationHandler;
import com.authguard.rest.routes.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.inject.Injector;
import io.javalin.Javalin;
import com.authguard.rest.routes.*;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.path;

class Server {
    private final static Logger log = LoggerFactory.getLogger(Server.class.getSimpleName());

    private final Injector injector;

    Server(final Injector injector) {
        this.injector = injector;
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
            path("/otp", injector.getInstance(OtpRoute.class));
            path("/keys", injector.getInstance(ApiKeysRoute.class));
            path("/accounts", injector.getInstance(AccountsRoute.class));
            path("/apps", injector.getInstance(ApplicationsRoute.class));
            path("/admin", injector.getInstance(AdminRoute.class));
            path("/verification", injector.getInstance(VerificationRoute.class));
        });

        // if we failed to process a request body
        app.exception(JsonMappingException.class, (e, context) -> context.status(422).result("Unprocessable entity"));

        app.exception(ServiceException.class, (e, context) -> context.status(400).result(e.toString()));
        app.exception(ServiceAuthorizationException.class, (e, context) -> context.status(401));

        app.exception(Exception.class, (e, context) -> {
            log.error("An exception was thrown", e);
            context.status(500).result("Internal server error");
        });

        // run
        app.start(port);
    }
}
