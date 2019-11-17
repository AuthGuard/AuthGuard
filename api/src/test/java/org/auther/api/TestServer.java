package org.auther.api;

import com.auther.config.ConfigContext;
import com.auther.config.JacksonConfigContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.auther.api.injectors.ConfigBinder;
import org.auther.api.injectors.MappersBinder;

import java.io.File;

class TestServer {
    private int port;

    private final Injector injector;
    private final Javalin app;

    private Server server;

    TestServer() {
        final ConfigContext configContext = new JacksonConfigContext(
                new File(Application.class.getClassLoader().getResource("application.json").getFile())
        ).getSubContext(ConfigContext.ROOT_CONFIG_PROPERTY);

        injector = Guice.createInjector(new MocksBinder(), new MappersBinder(), new ConfigBinder(configContext));
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
