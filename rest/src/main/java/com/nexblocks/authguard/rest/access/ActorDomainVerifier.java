package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.service.model.AppBO;
import io.javalin.http.Context;

// TODO move to Requester
public class ActorDomainVerifier {
    public static boolean verifyActorDomain(final Context context, final String domain) {
        if (context.attribute("actor") instanceof AppBO) {
            final AppBO actor = context.attribute("actor");

            if (actor == null) {
                context.status(401)
                        .json(new Error("", "Actor was missing"));

                return false;
            }

            return verifyActorDomain(actor, context, domain);
        }

        return true;
    }

    public static boolean verifyActorDomain(final AppBO actor, final Context context,
                                            final String requestDomain) {
        final boolean isAuthClient = actor.getRoles().contains(AuthGuardRoles.AUTH_CLIENT);

        if (isAuthClient && !actor.getDomain().equals(requestDomain)) {
            context.status(403)
                    .json(new Error("", "An auth client violated its restrictions in the request"));

            return false;
        }

        return true;
    }

    public static boolean verifyAuthClientDomain(final AppBO actor, final Context context,
                                                 final String requestDomain) {
        if (!actor.getDomain().equals(requestDomain)) {
            context.status(403)
                    .json(new Error("", "An auth client violated its restrictions in the request"));

            return false;
        }

        return true;
    }
}
