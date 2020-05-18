package com.authguard.rest.server;

import com.authguard.config.ConfigContext;
import com.authguard.emb.AutoSubscribers;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private final static Logger log = LoggerFactory.getLogger(Server.class.getSimpleName());

    private final Injector injector;
    private final ConfigContext configContext;

    public Server(final Injector injector, final ConfigContext configContext) {
        this.injector = injector;
        this.configContext = configContext;
    }

    public void start(final Javalin app, final int port) {
        log.info("Configuring server");

        final ServerMiddlewareHandlers middleware = new ServerMiddlewareHandlers(injector);
        final ServerExceptionHandlers exceptions = new ServerExceptionHandlers();
        final ServerRoutesHandlers routes = new ServerRoutesHandlers(injector, configContext);

        middleware.configureFor(app);

        exceptions.configureFor(app);

        routes.configureFor(app);

        // initialize subscribers
        injector.getInstance(AutoSubscribers.class).subscribe();

        log.info("Configuration complete");

        // run
        app.start(port);
    }
}
