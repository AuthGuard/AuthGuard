package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.bindings.PluginsRegistry;
import com.nexblocks.authguard.config.ConfigContext;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class AdminHandler implements VertxApiHandler {
    private final ConfigContext configContext;

    @Inject
    public AdminHandler(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public void register(final Router router) {
        router.get("/admin/config")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getConfig);

        router.get("/admin/bindings")
                .handler(VertxRolesAccessHandler.onlyAdminClient())
                .handler(this::getConfig);

    }

    public void getConfig(final RoutingContext context) {
        context.response()
                .end(Json.encode(this.configContext.asMap()));
    }

    public void getBindings(final RoutingContext context) {
        context.response()
                .end(Json.encode(PluginsRegistry.getBindingsGroupedByPackage()));
    }
}
