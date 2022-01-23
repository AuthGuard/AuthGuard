package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class CredentialsApi implements ApiRoute {

    @Override
    public String getPath() {
        return "credentials";
    }

    @Override
    public void addEndpoints() {
        get("/:id", this::getById, ActorRoles.adminClient());
        get("/domain/:domain/identifier/:identifier", this::getByIdentifier, ActorRoles.adminClient());
        get("/domain/:domain/identifier/:identifier/exists", this::identifierExists, ActorRoles.adminOrAuthClient());

        post("/", this::create, ActorRoles.of("authguard_admin_client", "one_time_admin"));

        put("/:id", this::update, ActorRoles.adminClient());
        patch("/:id/password", this::updatePassword, ActorRoles.adminClient());

        patch("/:id/identifiers", this::addIdentifiers, ActorRoles.adminClient());
        delete("/:id/identifiers", this::removeIdentifiers, ActorRoles.adminClient());

        delete("/:id", this::removeById, ActorRoles.adminClient());

        post("/reset_token", this::createResetToken, ActorRoles.adminOrAuthClient());
        post("/reset", this::resetPassword, ActorRoles.adminOrAuthClient());
    }

    public abstract void create(final Context context);

    public abstract void update(final Context context);

    public abstract void updatePassword(final Context context);

    public abstract void addIdentifiers(final Context context);

    public abstract void removeIdentifiers(final Context context);

    public abstract void getById(final Context context);

    public abstract void getByIdentifier(final Context context);

    public abstract void identifierExists(final Context context);

    public abstract void removeById(final Context context);

    public abstract void createResetToken(final Context context);

    public abstract void resetPassword(final Context context);
}