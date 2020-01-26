package com.authguard.rest.routes;

import com.authguard.config.ConfigContext;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import com.authguard.rest.access.ActorRole;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.core.security.SecurityUtil.roles;

public class AdminRoute implements EndpointGroup {
    private final ConfigContext configContext;

    @Inject
    public AdminRoute(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public void addEndpoints() {
        get("/config", this::getConfig, roles(ActorRole.of("admin")));
    }

    private void getConfig(final Context context) {
        context.status(200).json(this.configContext.get("authguard"));
    }
}
