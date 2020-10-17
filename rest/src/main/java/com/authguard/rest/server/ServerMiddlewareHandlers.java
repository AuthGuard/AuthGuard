package com.authguard.rest.server;

import com.authguard.rest.access.AuthorizationHandler;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMiddlewareHandlers implements JavalinAppConfigurer {
    private final static Logger log = LoggerFactory.getLogger(AuthGuardServer.class.getSimpleName());

    private final Injector injector;

    public ServerMiddlewareHandlers(final Injector injector) {
        this.injector = injector;
    }

    @Override
    public void configure(final Javalin app) {
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
    }
}
