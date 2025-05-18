package com.nexblocks.authguard.rest;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.nexblocks.authguard.api.routes.ApiRoute;
import com.nexblocks.authguard.bindings.ApiRoutesBinder;
import com.nexblocks.authguard.bindings.ConfigBinder;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.rest.bindings.MappersBinder;
import com.nexblocks.authguard.rest.config.ImmutableServerConfig;
import com.nexblocks.authguard.rest.server.AuthGuardServer;
import com.nexblocks.authguard.rest.vertx.RoutesBindings;
import io.javalin.Javalin;

import java.util.Collections;
import java.util.List;

import static io.javalin.apibuilder.ApiBuilder.path;

class TestServer {
    private int port;

    private final ConfigContext configContext;
    private final Injector injector;
    private final Javalin app;

    private AuthGuardServer authGuardServer;

    TestServer() {
        this.configContext = new ConfigurationLoader().loadFromResources();

        injector = Guice.createInjector(
                new MocksBinder(),
                new MappersBinder(),
                new ConfigBinder(configContext),
                new ApiRoutesBinder(Collections.singleton("com.nexblocks.authguard"), configContext),
                new RoutesBindings(Collections.singleton("com.nexblocks.authguard"), configContext));

        List<Binding<ApiRoute>> routeBindings =
                injector.findBindingsByType(TypeLiteral.get(ApiRoute.class));

        app = Javalin.create(config -> {
            config.router.apiBuilder(() -> {
                routeBindings.forEach(binding -> {
                    final ApiRoute route = binding.getProvider().get();

                    System.out.printf("Binding path /%s to route %s%n", route.getPath(), route);

                    path("/" + route.getPath(), route);
                });
            });
        });

        app.beforeMatched(new TestAccessManager());

        port = 7000;
    }

    void start() {
        authGuardServer = new AuthGuardServer(injector, ImmutableServerConfig.builder()
                .build());

        authGuardServer.start(app, port);
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
