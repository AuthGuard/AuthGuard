package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class KeyManagementSystemApi implements ApiRoute {

    @Override
    public String getPath() {
        return "/domains/{domain}/kms";
    }

    @Override
    public void addEndpoints() {
        post("/generator", this::generate, ActorRoles.adminClient());
        get("/keys", this::getByDomain, ActorRoles.adminClient());
        get("/keys/{id}", this::getById, ActorRoles.adminClient());
        delete("/keys/{id}", this::deleteById, ActorRoles.adminClient());
    }

    public abstract void generate(final Context context);
    public abstract void getByDomain(final Context context);

    public abstract void getById(final Context context);

    public abstract void deleteById(final Context context);
}
