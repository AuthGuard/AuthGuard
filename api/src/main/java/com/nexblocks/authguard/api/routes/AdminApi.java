package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;

public abstract class AdminApi implements ApiRoute {

    @Override
    public String getPath() {
        return "admin";
    }

    @Override
    public void addEndpoints() {
        get("/config", this::getConfig, ActorRoles.adminClient());
        get("/bindings", this::getBindings, ActorRoles.adminClient());
    }

    public abstract void getConfig(final Context context);

    public abstract void getBindings(final Context context);
}
