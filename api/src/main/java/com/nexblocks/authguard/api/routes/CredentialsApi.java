package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class CredentialsApi implements ApiRoute {

    @Override
    public String getPath() {
        return "/domains/{domain}/credentials";
    }

    @Override
    public void addEndpoints() {
        patch("/{id}/password", this::updatePassword, ActorRoles.adminClient());

        patch("/{id}/identifiers", this::addIdentifiers, ActorRoles.adminClient());
        delete("/{id}/identifiers", this::removeIdentifiers, ActorRoles.adminClient());

        post("/reset_token", this::createResetToken, ActorRoles.adminOrAuthClient());
        post("/reset", this::resetPassword, ActorRoles.adminOrAuthClient());
    }

    public abstract void updatePassword(final Context context);

    public abstract void addIdentifiers(final Context context);

    public abstract void removeIdentifiers(final Context context);

    public abstract void createResetToken(final Context context);

    public abstract void resetPassword(final Context context);
}