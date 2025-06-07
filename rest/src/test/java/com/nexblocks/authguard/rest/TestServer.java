package com.nexblocks.authguard.rest;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nexblocks.authguard.bindings.ConfigBinder;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.rest.bindings.MappersBinder;
import com.nexblocks.authguard.rest.config.ImmutableServerConfig;
import com.nexblocks.authguard.rest.server.AuthGuardServer;
import com.nexblocks.authguard.rest.vertx.RoutesBindings;
import io.vertx.core.Vertx;

import java.util.Collections;

class TestServer {
    private int port;

    private final ConfigContext configContext;
    private final Injector injector;

    private AuthGuardServer authGuardServer;

    TestServer() {
        this.configContext = new ConfigurationLoader().loadFromResources();

        injector = Guice.createInjector(
                new MocksBinder(),
                new MappersBinder(),
                new ConfigBinder(configContext),
                new RoutesBindings(Collections.singleton("com.nexblocks.authguard"), configContext));

        Vertx vertx = Vertx.vertx();

        port = 7000;
    }

    void start() {
        authGuardServer = new AuthGuardServer(injector, ImmutableServerConfig.builder()
                .build());
    }

    <T> T getMock(Class<T> clazz) {
        return injector.getInstance(clazz);
    }

    int getPort() {
        return port;
    }
}
