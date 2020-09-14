package com.authguard.api.routes;

import com.authguard.api.access.ActorRoles;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class AuthApi implements EndpointGroup {
    
    @Override
    public void addEndpoints() {
        post("/authenticate", this::authenticate, ActorRoles.adminClient());
        post("/exchange", this::exchange, ActorRoles.adminClient());
    }

    public abstract void authenticate(final Context context);

    public abstract void exchange(final Context context);
}
