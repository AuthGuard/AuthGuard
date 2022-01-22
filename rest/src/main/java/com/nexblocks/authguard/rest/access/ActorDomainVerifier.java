package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.service.model.AppBO;
import io.javalin.http.Context;

public class ActorDomainVerifier {
    public static boolean verifyActorDomain(final Context context, final String domain) {
        if (context.attribute("actor") instanceof AppBO) {
            final AppBO actor = context.attribute("actor");
            final boolean isAuthClient = actor.getRoles().contains(AuthGuardRoles.AUTH_CLIENT);

            if (isAuthClient && !actor.getDomain().equals(domain)) {
                context.status(403)
                        .json(new Error("", "An auth client violated its restrictions in the request"));

                return false;
            }

            return true;
        }

        return true;
    }
}
