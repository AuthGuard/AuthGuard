package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;

public abstract class EventsApi implements ApiRoute {
    @Override
    public String getPath() {
        return "/domains/:domain/events";
    }

    @Override
    public void addEndpoints() {
        get("/", this::getByDomain, ActorRoles.adminClient());
    }

    public abstract void getByDomain(Context context);
}
