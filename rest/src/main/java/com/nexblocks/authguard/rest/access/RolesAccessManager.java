package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.access.ActorRole;
import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.ClientBO;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

public class RolesAccessManager implements Handler {
    private static final Logger LOG = LoggerFactory.getLogger(RolesAccessManager.class);
    private final Set<String> unprotectedPaths;

    public RolesAccessManager(Set<String> unprotectedPaths) {
        this.unprotectedPaths = unprotectedPaths;
    }

    @Override
    public void handle(@NotNull final Context context) throws Exception {
        Set<RouteRole> permittedRoles = context.routeRoles();
        final Object actor = context.attribute("actor");

        if (!isPermitted(actor, permittedRoles, context)) {
            throw new UnauthorizedResponse();
        }
    }

    private boolean isPermitted(final Object actor, final Set<RouteRole> permittedRoles,
                                final Context context) {
        if (actor instanceof AccountBO) {
            return isPermitted((AccountBO) actor, permittedRoles);
        } else if (actor instanceof ClientBO) {
            return isPermitted((ClientBO) actor, permittedRoles);
        }

        final String[] pathParts = context.path().split("/");

        return unprotectedPaths.contains(pathParts[1]);
    }

    private boolean isPermitted(final AccountBO actor, final Set<RouteRole> permittedRoles) {
        if (actor == null) {
            return false;
        }

        final Optional<ActorRole> matchedRole = actor.getRoles().stream()
                .map(ActorRole::of)
                .filter(permittedRoles::contains)
                .findFirst(); // we only need to have one valid role to access the endpoint

        return matchedRole.isPresent();
    }

    private boolean isPermitted(final ClientBO actor, final Set<RouteRole> permittedRoles) {
        if (actor == null) {
            return false;
        }

        switch (actor.getClientType()) {
            case AUTH: return permittedRoles.contains(ActorRole.of(AuthGuardRoles.AUTH_CLIENT));
            case ADMIN: return permittedRoles.contains(ActorRole.of(AuthGuardRoles.ADMIN_CLIENT));
            case SSO: return permittedRoles.contains(ActorRole.of(AuthGuardRoles.SSO_CLIENT));
            default:
                LOG.error("Undefined client type {}", actor.getClientType());

                return false;
        }
    }
}
