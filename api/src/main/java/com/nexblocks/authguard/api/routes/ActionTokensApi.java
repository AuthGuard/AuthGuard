package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class ActionTokensApi implements ApiRoute {
    @Override
    public String getPath() {
        return "actions";
    }

    @Override
    public void addEndpoints() {
        post("/otp", this::createOtp, ActorRoles.adminClient());
        post("/token", this::createToken, ActorRoles.adminClient());
        post("/verify", this::verifyToken, ActorRoles.adminClient());
    }

    public abstract void createOtp(final Context context);
    public abstract void createToken(final Context context);
    public abstract void verifyToken(final Context context);
}
