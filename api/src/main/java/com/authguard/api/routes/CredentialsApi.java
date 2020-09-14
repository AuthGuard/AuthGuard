package com.authguard.api.routes;

import com.authguard.api.access.ActorRoles;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class CredentialsApi implements EndpointGroup {

    @Override
    public void addEndpoints() {
        get("/:id", this::getById, ActorRoles.adminClient());

        post("/", this::create, ActorRoles.of("authguard_admin_client", "one_time_admin"));

        put("/:id", this::update, ActorRoles.adminClient());
        patch("/:id/password", this::updatePassword, ActorRoles.adminClient());

        patch("/:id/identifiers", this::addIdentifiers, ActorRoles.adminClient());
        delete("/:id/identifiers", this::removeIdentifiers, ActorRoles.adminClient());

        delete("/:id", this::removeById, ActorRoles.adminClient());
    }

    public abstract void create(final Context context);

    public abstract void update(final Context context);

    public abstract void updatePassword(final Context context);

    public abstract void addIdentifiers(final Context context);

    public abstract void removeIdentifiers(final Context context);

    public abstract void getById(final Context context);

    public abstract void removeById(final Context context);
}