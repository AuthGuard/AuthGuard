package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.service.Domains;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.ClientBO;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DomainAuthorizationHandler implements Handler {
    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        Object actor = ctx.attribute("actor");

        if (actor == null) {
            return;
        }

        String domain = ctx.pathParam("domain");

        if (actor instanceof AccountBO) {
            String actorDomain = ((AccountBO) actor).getDomain();

            if (Objects.equals(actorDomain, domain) || Objects.equals(actorDomain, Domains.GLOBAL_RESERVED_DOMAIN)) {
                return;
            }
        } else if (actor instanceof ClientBO) {
            String actorDomain = ((ClientBO) actor).getDomain();

            if (Objects.equals(actorDomain, domain) || Objects.equals(actorDomain, Domains.GLOBAL_RESERVED_DOMAIN)) {
                return;
            }
        }

        ctx.status(403).json(new Error("401", "Domain is outside the scope of the actor"));
    }
}
