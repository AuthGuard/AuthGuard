package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public abstract class AuthApi implements ApiRoute {

    @Override
    public String getPath() {
        return "auth";
    }

    @Override
    public void addEndpoints() {
        post("/authenticate", this::authenticate, ActorRoles.adminOrAuthClient());
        post("/logout", this::logout, ActorRoles.adminOrAuthClient());
        post("/exchange", this::exchange, ActorRoles.adminClient());
        post("/exchange/clear", this::clearToken, ActorRoles.adminClient());
        get("/exchange/attempts", this::getExchangeAttempts, ActorRoles.adminClient());
    }

    public abstract void authenticate(final Context context);

    public abstract void logout(final Context context);

    public abstract void exchange(final Context context);

    public abstract void clearToken(final Context context);

    public abstract void getExchangeAttempts(final Context context);
}
