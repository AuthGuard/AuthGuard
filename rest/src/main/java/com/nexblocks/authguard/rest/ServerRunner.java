package com.nexblocks.authguard.rest;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.nexblocks.authguard.api.routes.ApiRoute;
import com.nexblocks.authguard.bindings.*;
import com.nexblocks.authguard.bootstrap.BootstrapRunner;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.injection.ClassSearch;
import com.nexblocks.authguard.rest.bindings.MappersBinder;
import com.nexblocks.authguard.rest.config.ImmutableServerConfig;
import com.nexblocks.authguard.rest.server.AuthGuardServer;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.path;

public class ServerRunner {
    private final static Logger log = LoggerFactory.getLogger(ServerRunner.class);

    public void run(final ConfigContext configContext, final boolean disableBootstrap,
                    final boolean disableServer) {
        // class search
        final Collection<String> searchPackages = configContext.getSubContext("injection")
                .getAsCollection("packages", String.class);

        final ClassSearch classSearch = new ClassSearch(searchPackages);

        // injectors
        final Injector injector = Guice.createInjector(new MappersBinder(),
                new ConfigBinder(configContext),
                new ExchangesBinder(configContext, searchPackages),
                new ApiKeysExchangeBinder(configContext, searchPackages),
                new ApiRoutesBinder(searchPackages, configContext),
                new ServicesBinder(configContext),
                new JwtBinder(configContext),
                new DalBinder(configContext, searchPackages),
                new EmbBinder(searchPackages),
                new ExternalProvidersBinder(configContext, searchPackages));

        log.info("Initialed injection binders");

        // run bootstraps
        if (!disableBootstrap) {
            new BootstrapRunner(classSearch, injector).runAll();

            log.info("Completed bootstrap");
        } else {
            log.info("Skipping bootstrap steps");
        }

        // run the server
        if (disableServer) {
            log.info("The server was disabled. Skipping.");

            return;
        }

        final ImmutableServerConfig serverConfig = Optional.ofNullable(configContext.getAsConfigBean("server", ImmutableServerConfig.class))
                .orElseGet(() -> ImmutableServerConfig.builder()
                        .port(3000)
                        .build());

        new AuthGuardServer(injector, serverConfig).start(Javalin.create(config -> {
            if (serverConfig.enforceSsl()) {
                config.bundledPlugins.enableSslRedirects();
            }

            List<Binding<ApiRoute>> routeBindings =
                    injector.findBindingsByType(TypeLiteral.get(ApiRoute.class));

            config.jsonMapper(new JavalinJackson()
                    .updateMapper(mapper -> mapper.registerModule(new JavaTimeModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

            config.router.apiBuilder(() -> {
                routeBindings.forEach(binding -> {
                    final ApiRoute route = binding.getProvider().get();

                    log.info("Binding path /{} to route {}", route.getPath(), route);

                    path("/" + route.getPath(), route);
                });
            });
        }), serverConfig.getPort());
    }
}
