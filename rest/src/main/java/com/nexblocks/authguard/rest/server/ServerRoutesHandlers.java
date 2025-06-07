package com.nexblocks.authguard.rest.server;

import com.google.inject.Injector;
import io.javalin.Javalin;

@Deprecated
public class ServerRoutesHandlers implements JavalinAppConfigurer {
    public ServerRoutesHandlers(final Injector injector) {

    }

    @Override
    public void configure(final Javalin app) {
    }
}
