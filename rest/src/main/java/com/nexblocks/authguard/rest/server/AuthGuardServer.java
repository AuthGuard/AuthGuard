package com.nexblocks.authguard.rest.server;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Injector;
import com.nexblocks.authguard.emb.AutoSubscribers;
import com.nexblocks.authguard.rest.access.RolesAccessManager;
import com.nexblocks.authguard.rest.config.ServerConfig;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthGuardServer {
    private final static Logger LOG = LoggerFactory.getLogger(AuthGuardServer.class);

    private final Injector injector;
    private final ServerConfig serverConfig;

    public AuthGuardServer(final Injector injector, final ServerConfig serverConfig) {
        this.injector = injector;
        this.serverConfig = serverConfig;
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

        app.beforeMatched(new RolesAccessManager(serverConfig.getUnprotectedPaths()));

        final ServerMiddlewareHandlers middleware = new ServerMiddlewareHandlers(injector);
        final ServerExceptionHandlers exceptions = new ServerExceptionHandlers();

        middleware.configure(app);

        exceptions.configure(app);

        JavalinJackson.defaultMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // initialize subscribers
        injector.getInstance(AutoSubscribers.class).subscribe();

        LOG.info("Configuration complete");
    }
}
