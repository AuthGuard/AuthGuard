package com.nexblocks.authguard.api.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class TotpApi implements ApiRoute {

    @Override
    public String getPath() {
        return "/domains/:domain/totp";
    }

    @Override
    public void addEndpoints() {
        post("/generate", this::generate, ActorRoles.adminClient());
        get("/:accountId/keys", this::getByAccountId, ActorRoles.adminClient());
        delete("/keys/:id", this::deleteById, ActorRoles.adminClient());
    }

    public abstract void generate(final Context context);
    public abstract void getByAccountId(final Context context);
    public abstract void deleteById(final Context context);
}
