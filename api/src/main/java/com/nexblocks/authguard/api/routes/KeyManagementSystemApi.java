package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class KeyManagementSystemApi implements ApiRoute {

    @Override
    public String getPath() {
        return "/domains/:domain/kms";
    }

    @Override
    public void addEndpoints() {
        post("/", this::generate, ActorRoles.anyAdmin());
        get("/:id", this::getById, ActorRoles.adminClient());
        delete("/:id", this::deleteById, ActorRoles.adminClient());
    }

    public abstract void generate(final Context context);

    public abstract void getById(final Context context);

    public abstract void deleteById(final Context context);
}
