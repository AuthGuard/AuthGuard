package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class ApplicationsApi implements ApiRoute {

    @Override
    public String getPath() {
        return "/domains/{domain}/apps";
    }

    @Override
    public void addEndpoints() {
        post("/", this::create, ActorRoles.anyAdmin());

        get("/{id}", this::getById, ActorRoles.adminClient());
        get("/externalId/{id}", this::getByExternalId, ActorRoles.adminClient());
        put("/{id}", this::update, ActorRoles.adminClient());
        delete("/{id}", this::deleteById, ActorRoles.adminClient());

        patch("/{id}/activate", this::activate, ActorRoles.adminClient());
        patch("/{id}/deactivate", this::deactivate, ActorRoles.adminClient());
        patch("/{id}/permissions", this::updatePermissions, ActorRoles.adminClient());
        patch("/{id}/roles", this::updateRoles, ActorRoles.adminClient());

        get("/{id}/keys", this::getApiKeys, ActorRoles.adminClient());
        get("/{id}/crypto_keys", this::getCryptoKeys, ActorRoles.adminClient());
    }

    public abstract void create(final Context context);

    public abstract void getById(final Context context);

    public abstract void getByExternalId(final Context context);

    public abstract void update(final Context context);

    public abstract void deleteById(final Context context);

    public abstract void activate(final Context context);

    public abstract void deactivate(final Context context);

    public abstract void updatePermissions(final Context context);

    public abstract void updateRoles(final Context context);

    public abstract void getApiKeys(final Context context);
    public abstract void getCryptoKeys(final Context context);
}
