package org.auther.api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.auther.api.injectors.ConfigBinder;
import org.auther.api.injectors.MappersBinder;

class TestServer {
    private int port;

    private final Injector injector;
    private final Javalin app;

    private Server server;

    TestServer() {
        injector = Guice.createInjector(new MocksBinder(), new MappersBinder(), new ConfigBinder());
        app = Javalin.create();
    }

    void start() {
        server = new Server(injector);

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
