package com.nexblocks.authguard.rest.server;

import com.nexblocks.authguard.emb.AutoSubscribers;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Injector;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthGuardServer {
    private final static Logger LOG = LoggerFactory.getLogger(AuthGuardServer.class);

    private final Injector injector;

    public AuthGuardServer(final Injector injector) {
        this.injector = injector;
    }

    public void start(final Javalin app) {
        configure(app);

        app.start();
    }

    public void start(final Javalin app, final int port) {
        configure(app);

        app.start(port);
    }

    private void configure(final Javalin app) {
        LOG.info("Configuring server");

        final ServerMiddlewareHandlers middleware = new ServerMiddlewareHandlers(injector);
        final ServerExceptionHandlers exceptions = new ServerExceptionHandlers();
        final ServerRoutesHandlers routes = new ServerRoutesHandlers(injector);

        middleware.configure(app);

        exceptions.configure(app);

        routes.configure(app);

        JavalinJackson.getObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // initialize subscribers
        injector.getInstance(AutoSubscribers.class).subscribe();

        LOG.info("Configuration complete");
    }
}
