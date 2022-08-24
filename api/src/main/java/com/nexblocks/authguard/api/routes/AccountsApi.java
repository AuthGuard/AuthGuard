package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class AccountsApi implements ApiRoute {

    @Override
    public String getPath() {
        return "accounts";
    }

    public void addEndpoints() {
        post("/", this::create, ActorRoles.of("authguard_admin_client", "one_time_admin", "authguard_auth_client"));

        get("/:id", this::getById, ActorRoles.adminClient());
        delete("/:id", this::deleteAccount, ActorRoles.adminClient());
        patch("/:id", this::patchAccount, ActorRoles.adminClient());
        get("/domain/:domain/identifier/:identifier", this::getByIdentifier, ActorRoles.adminClient());
        get("/domain/:domain/identifier/:identifier/exists", this::identifierExists, ActorRoles.adminOrAuthClient());

        get("/externalId/:id", this::getByExternalId, ActorRoles.adminClient());
        get("/domain/:domain/email/:email", this::getByEmail, ActorRoles.adminClient());
        get("/domain/:domain/email/:email/exists", this::emailExists, ActorRoles.adminOrAuthClient());

        patch("/:id/permissions", this::updatePermissions, ActorRoles.adminClient());
        patch("/:id/roles", this::updateRoles, ActorRoles.adminClient());
        get("/:id/apps", this::getApps, ActorRoles.adminClient());

        patch("/:id/activate", this::activate, ActorRoles.adminClient());
        patch("/:id/deactivate", this::deactivate, ActorRoles.adminClient());

        get("/:id/locks", this::getActiveLocks, ActorRoles.adminClient());
    }

    public abstract void create(final Context context);


    public abstract void getById(final Context context);

    public abstract void getByIdentifier(final Context context);

    public abstract void identifierExists(final Context context);

    public abstract void deleteAccount(final Context context);

    public abstract void patchAccount(final Context context);

    public abstract void getByExternalId(final Context context);

    public abstract void getByEmail(final Context context);

    public abstract void emailExists(final Context context);

    public abstract void updatePermissions(final Context context);

    public abstract void updateRoles(final Context context);

    public abstract void getApps(final Context context);

    public abstract void activate(final Context context);

    public abstract void deactivate(final Context context);

    public abstract void getActiveLocks(final Context context);
}
