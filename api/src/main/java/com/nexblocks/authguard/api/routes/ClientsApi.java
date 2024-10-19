package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class ClientsApi implements ApiRoute {
    @Override
    public String getPath() {
        return "/domains/{domain}/clients";
    }

    @Override
    public void addEndpoints() {
        post("/", this::create, ActorRoles.anyAdmin());
        get("/", this::getByDomain, ActorRoles.adminClient());

        get("/{id}", this::getById, ActorRoles.adminClient());
        get("/externalId/{id}", this::getByExternalId, ActorRoles.adminClient());
        delete("/{id}", this::deleteById, ActorRoles.adminClient());

        patch("/{id}/activate", this::activate, ActorRoles.adminClient());
        patch("/{id}/deactivate", this::deactivate, ActorRoles.adminClient());

        get("/{id}/keys", this::getApiKeys, ActorRoles.adminClient());
    }

    public abstract void create(final Context context);
    public abstract void getByDomain(final Context context);

    public abstract void getById(final Context context);

    public abstract void getByExternalId(final Context context);


    public abstract void deleteById(final Context context);

    public abstract void activate(final Context context);

    public abstract void deactivate(final Context context);

    public abstract void getApiKeys(final Context context);
}
