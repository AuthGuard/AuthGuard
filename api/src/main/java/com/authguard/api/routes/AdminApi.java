package com.authguard.api.routes;

import com.authguard.api.access.ActorRole;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.core.security.SecurityUtil.roles;

public abstract class AdminApi implements EndpointGroup {

    @Override
    public void addEndpoints() {
        get("/config", this::getConfig, roles(ActorRole.of("admin")));
    }

    public abstract void getConfig(final Context context);
}
