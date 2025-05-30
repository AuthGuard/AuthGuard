package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.model.ClientBO;
import io.javalin.http.Context;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class ActorDomainVerifier {
    public static boolean verifyActorDomain(final Context context, final String domain) {
        if (context.attribute("actor") instanceof ClientBO) {
            final ClientBO actor = context.attribute("actor");

            if (actor == null) {
                context.status(401)
                        .json(new Error("", "Actor was missing"));

                return false;
            }

            return verifyActorDomain(actor, context, domain);
        }

        return true;
    }

    public static boolean verifyActorDomain(final RoutingContext context, final String domain) {
        if (context.get("actor") instanceof ClientBO) {
            final ClientBO actor = context.get("actor");

            if (actor == null) {
                context.response().setStatusCode(403)
                        .putHeader("Content-Type", "application/json")
                        .end(Json.encode(new Error("", "Actor was missing")));

                return false;
            }

            return verifyActorDomain(actor, context, domain);
        }

        return true;
    }

    public static boolean verifyActorDomain(final ClientBO actor, final Context context,
                                            final String requestDomain) {
        final boolean isAuthClient = actor.getClientType() == Client.ClientType.AUTH;

        if (isAuthClient && !actor.getDomain().equals(requestDomain)) {
            context.status(403)
                    .json(new Error("", "An auth client violated its restrictions in the request"));

            return false;
        }

        return true;
    }

    public static boolean verifyActorDomain(final ClientBO actor, final RoutingContext context,
                                            final String requestDomain) {
        final boolean isAuthClient = actor.getClientType() == Client.ClientType.AUTH;

        if (isAuthClient && !actor.getDomain().equals(requestDomain)) {
            context.response().setStatusCode(403)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new Error("", "An auth client violated its restrictions in the request")));

            return false;
        }

        return true;
    }

    public static boolean verifyAuthClientDomain(final ClientBO actor, final Context context,
                                                 final String requestDomain) {
        if (!actor.getDomain().equals(requestDomain)) {
            context.status(403)
                    .json(new Error("", "An auth client violated its restrictions in the request"));

            return false;
        }

        return true;
    }

    public static boolean verifyAuthClientDomain(final ClientBO actor, final RoutingContext context,
                                                 final String requestDomain) {
        if (!actor.getDomain().equals(requestDomain)) {
            context.response().setStatusCode(403)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encode(new Error("", "An auth client violated its restrictions in the request")));

            return false;
        }

        return true;
    }
}
