package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;

public abstract class GeneralApi implements ApiRoute {

    @Override
    public String getPath() {
        return "general";
    }

    @Override
    public void addEndpoints() {
        get("/heartbeat", this::heartbeat, ActorRoles.adminClient());
    }

    public abstract void heartbeat(final Context context);
}
