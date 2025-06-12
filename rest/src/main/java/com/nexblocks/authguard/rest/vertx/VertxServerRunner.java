package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nexblocks.authguard.bindings.*;
import com.nexblocks.authguard.bootstrap.BootstrapRunner;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.injection.ClassSearch;
import com.nexblocks.authguard.rest.bindings.MappersBinder;
import com.nexblocks.authguard.rest.config.ImmutableServerConfig;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

public class VertxServerRunner {
    private final static Logger log = LoggerFactory.getLogger(VertxServerRunner.class);

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
                new RoutesBindings(searchPackages, configContext),
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

        Vertx vertx = Vertx.vertx(); // TODO add a config section for vertx

        new VertxAuthGuardServer(injector, serverConfig).start(vertx);
    }

}
