package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class TrackingSessionsApi implements ApiRoute {

    @Override
    public String getPath() {
        return "/domains/{domain}/tracking_sessions";
    }

    @Override
    public void addEndpoints() {
        delete("/{token}", this::deleteById, ActorRoles.adminClient());
    }

    public abstract void deleteById(final Context context);
}
