package com.authguard.rest.routes;

import com.authguard.api.routes.AdminApi;
import com.authguard.config.ConfigContext;
import com.google.inject.Inject;
import io.javalin.http.Context;

public class AdminRoute extends AdminApi {
    private final ConfigContext configContext;

    @Inject
    public AdminRoute(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    public void getConfig(final Context context) {
        context.status(200).json(this.configContext.get("authguard"));
    }
}
