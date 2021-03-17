package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.routes.AdminApi;
import com.nexblocks.authguard.bindings.PluginsRegistry;
import com.nexblocks.authguard.config.ConfigContext;
import io.javalin.http.Context;

public class AdminRoute extends AdminApi {
    private final ConfigContext configContext;

    @Inject
    public AdminRoute(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    public void getConfig(final Context context) {
        context.status(200)
                .json(this.configContext.asMap());
    }

    @Override
    public void getBindings(final Context context) {
        context.status(200)
                .json(PluginsRegistry.getBindingsGroupedByPackage());
    }
}
