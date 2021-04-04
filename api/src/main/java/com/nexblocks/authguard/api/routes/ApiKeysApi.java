package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class ApiKeysApi implements ApiRoute {

    @Override
    public String getPath() {
        return "keys";
    }

    @Override
    public void addEndpoints() {
        post("/", this::generate, ActorRoles.anyAdmin());
    }

    public abstract void generate(final Context context);
}
