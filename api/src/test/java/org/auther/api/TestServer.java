package org.auther.api;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.auther.api.injectors.ConfigBinder;
import org.auther.api.injectors.MappersBinder;

class TestServer {
    private int port;

    private final Injector injector;

    TestServer() {
        injector = Guice.createInjector(new MocksBinder(), new MappersBinder(), new ConfigBinder());
    }

    void start() {
        final Javalin app = Javalin.create();
        final Server server = new Server(injector);

        this.port = app.port();

        server.start(app);
    }

    <T> T getMock(Class<T> clazz) {
        return injector.getInstance(clazz);
    }

    int getPort() {
        return port;
    }
}
