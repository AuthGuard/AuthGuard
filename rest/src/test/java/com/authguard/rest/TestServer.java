package com.authguard.rest;

import com.authguard.config.ConfigContext;
import com.authguard.config.JacksonConfigContext;
import com.authguard.rest.injectors.ConfigBinder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import com.authguard.rest.injectors.MappersBinder;

import java.io.File;

class TestServer {
    private int port;

    private final ConfigContext configContext;
    private final Injector injector;
    private final Javalin app;

    private Server server;

    TestServer() {
        this.configContext = new ConfigurationLoader().load();

        injector = Guice.createInjector(new MocksBinder(), new MappersBinder(), new ConfigBinder(configContext));
        app = Javalin.create();
    }

    void start() {
        server = new Server(injector, configContext);

        this.port = app.port();

        server.start(app, app.port());
    }

    void stop() {
        app.stop();
    }

    <T> T getMock(Class<T> clazz) {
        return injector.getInstance(clazz);
    }

    int getPort() {
        return port;
    }
}
