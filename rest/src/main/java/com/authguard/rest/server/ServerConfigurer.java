package com.authguard.rest.server;

import io.javalin.Javalin;

public interface ServerConfigurer {
    void configureFor(Javalin app);
}
