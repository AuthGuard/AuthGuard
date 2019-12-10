package org.auther.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.auther.rest.access.AuthorizationHandler;
import org.auther.rest.routes.AccountsRoute;
import org.auther.rest.routes.AdminRoute;
import org.auther.rest.routes.AuthRoute;
import org.auther.rest.routes.CredentialsRoute;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
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
            path("/accounts", injector.getInstance(AccountsRoute.class));
            path("/admin", injector.getInstance(AdminRoute.class));
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
