package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class VerificationApi implements ApiRoute {

    @Override
    public String getPath() {
        return "verification";
    }

    @Override
    public void addEndpoints() {
        post("/email", this::verifyEmail, ActorRoles.adminClient());
    }

    public abstract void verifyEmail(final Context context);
}
