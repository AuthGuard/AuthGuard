package com.authguard.api.routes;

import com.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class PasswordlessApi implements ApiRoute {

    @Override
    public String getPath() {
        return "passwordless";
    }

    @Override
    public void addEndpoints() {
        post("/verify", this::verify, ActorRoles.adminClient());
    }

    public abstract void verify(final Context context);
}
