package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import com.google.inject.Inject;
import com.nexblocks.authguard.api.routes.AdminApi;
import com.nexblocks.authguard.bindings.PluginsRegistry;
import com.nexblocks.authguard.config.ConfigContext;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;

public class AdminRoute extends AdminApi {
    private final ConfigContext configContext;

    @Inject
    public AdminRoute(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public void addEndpoints() {
        get("/config", this::getConfig, ActorRoles.adminClient());
        get("/bindings", this::getBindings, ActorRoles.adminClient());
    }

    public void getConfig(final Context context) {
        context.json(this.configContext.asMap());
    }

    @Override
    public void getBindings(final Context context) {
        context.json(PluginsRegistry.getBindingsGroupedByPackage());
    }
}
