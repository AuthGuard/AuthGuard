package com.nexblocks.authguard.rest.server;

import com.nexblocks.authguard.api.routes.ApiRoute;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.javalin.apibuilder.ApiBuilder.path;

@Deprecated
public class ServerRoutesHandlers implements JavalinAppConfigurer {
    private static final Logger LOG = LoggerFactory.getLogger(ServerRoutesHandlers.class);

    private final Injector injector;

    public ServerRoutesHandlers(final Injector injector) {
        this.injector = injector;
    }

    @Override
    public void configure(final Javalin app) {
        final List<Binding<ApiRoute>> routeBindings = injector.findBindingsByType(TypeLiteral.get(ApiRoute.class));

//        app.routes(() -> {
//            routeBindings.forEach(binding -> {
//                final ApiRoute route = binding.getProvider().get();
//
//                LOG.info("Binding path /{} to route {}", route.getPath(), route);
//
//                path("/" + route.getPath(), route);
//            });
//        });
    }
}
