package com.authguard.api.routes;

import com.authguard.api.access.ActorRoles;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class AccountsApi implements EndpointGroup {

    public void addEndpoints() {
        post("/", this::create, ActorRoles.of("authguard_admin_client", "one_time_admin"));

        get("/:id", this::getById, ActorRoles.adminClient());
        delete("/:id", this::deleteAccount, ActorRoles.adminClient());
        get("/externalId/:id", this::getByExternalId, ActorRoles.adminClient());

        patch("/:id/permissions", this::updatePermissions, ActorRoles.adminClient());
        patch("/:id/roles", this::updateRoles, ActorRoles.adminClient());

        patch("/:id/emails", this::addEmails, ActorRoles.adminClient());
        delete("/:id/emails", this::removeEmails, ActorRoles.adminClient());

        get("/:id/apps", this::getApps, ActorRoles.adminClient());

        patch("/:id/activate", this::activate, ActorRoles.adminClient());
        patch("/:id/deactivate", this::deactivate, ActorRoles.adminClient());
    }

    public abstract void create(final Context context);

    public abstract void getById(final Context context);

    public abstract void deleteAccount(final Context context);

    public abstract void getByExternalId(final Context context);

    public abstract void updatePermissions(final Context context);

    public abstract void updateRoles(final Context context);

    public abstract void addEmails(final Context context);

    public abstract void removeEmails(final Context context);

    public abstract void getApps(final Context context);

    public abstract void activate(final Context context);

    public abstract void deactivate(final Context context);
}
