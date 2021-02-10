package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.bindings.*;
import com.nexblocks.authguard.bootstrap.BootstrapRunner;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.injection.ClassSearch;
import com.nexblocks.authguard.rest.access.RolesAccessManager;
import com.nexblocks.authguard.rest.bindings.MappersBinder;
import com.nexblocks.authguard.rest.config.ImmutableServerConfig;
import com.nexblocks.authguard.rest.server.AuthGuardServer;
import com.nexblocks.authguard.rest.server.JettyServerProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nexblocks.authguard.bindings.*;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

public class ServerRunner {
    private final static Logger log = LoggerFactory.getLogger(ServerRunner.class);

    public void run(final ConfigContext configContext) {
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
        new BootstrapRunner(classSearch, injector).runAll();

        log.info("Completed bootstrap");

        // run the server
        final ImmutableServerConfig serverConfig = Optional.ofNullable(configContext.getAsConfigBean("server", ImmutableServerConfig.class))
                .orElseGet(() -> ImmutableServerConfig.builder()
                        .port(3000)
                        .build());

        new AuthGuardServer(injector).start(Javalin.create(config -> {
            config.enforceSsl = serverConfig.enforceSsl();

            config.server(() -> new JettyServerProvider(serverConfig).get());

            config.accessManager(new RolesAccessManager());
        }));
    }
}
