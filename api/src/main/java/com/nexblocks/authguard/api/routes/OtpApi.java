package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class OtpApi implements ApiRoute {

    @Override
    public String getPath() {
        return "otp";
    }

    @Override
    public void addEndpoints() {
        post("/verify", this::verify, ActorRoles.adminOrAuthClient());
    }

    public abstract void verify(final Context context);
}
