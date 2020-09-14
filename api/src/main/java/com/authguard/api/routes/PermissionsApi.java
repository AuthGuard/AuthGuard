package com.authguard.api.routes;

import com.authguard.api.access.ActorRoles;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class PermissionsApi implements EndpointGroup {

    @Override
    public void addEndpoints() {
        post("/", this::createPermission, ActorRoles.adminClient());
        get("/", this::getPermissions, ActorRoles.adminClient());
    }

    public abstract void createPermission(final Context context);

    public abstract void getPermissions(final Context context);
}
