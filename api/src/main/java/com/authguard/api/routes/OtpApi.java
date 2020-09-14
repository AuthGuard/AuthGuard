package com.authguard.api.routes;

import com.authguard.api.access.ActorRoles;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class OtpApi implements EndpointGroup {

    @Override
    public void addEndpoints() {
        post("/verify", this::verify, ActorRoles.adminClient());
    }

    public abstract void verify(final Context context);
}
