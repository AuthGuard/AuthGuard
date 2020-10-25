package com.authguard.api.routes;

import com.authguard.api.access.ActorRoles;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class RolesApi implements ApiRoute {

    @Override
    public String getPath() {
        return "roles";
    }

    @Override
    public void addEndpoints() {
        post("/", this::create, ActorRoles.adminClient());
        get("/:name", this::getByName, ActorRoles.adminClient());
        patch("/:name/permissions", this::updatePermissions, ActorRoles.adminClient());
    }

    public abstract void create(final Context context);

    public abstract void getByName(final Context context);

    public abstract void updatePermissions(final Context context);
}
