package com.authguard.rest;

import com.authguard.bindings.*;
import com.authguard.bootstrap.BootstrapRunner;
import com.authguard.config.ConfigContext;
import com.authguard.injection.ClassSearch;
import com.authguard.rest.access.RolesAccessManager;
import com.authguard.rest.bindings.MappersBinder;
import com.authguard.rest.server.Server;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

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
                new PasswordsBinder(configContext),
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
        new Server(injector, configContext).start(Javalin.create(config -> {
            config.accessManager(new RolesAccessManager());
        }), 3000);
    }
}
