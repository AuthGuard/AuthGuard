package com.nexblocks.authguard.rest.access;

import com.nexblocks.authguard.api.access.ActorRole;
import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.model.ClientBO;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

public class RolesAccessManager implements AccessManager {
    private static final Logger LOG = LoggerFactory.getLogger(RolesAccessManager.class);
    private final Set<String> unprotectedPaths;

    public RolesAccessManager(Set<String> unprotectedPaths) {
        this.unprotectedPaths = unprotectedPaths;
    }

    @Override
    public void manage(@NotNull final Handler handler, @NotNull final Context context,
                       @NotNull final Set<Role> permittedRoles) throws Exception {
        final Object actor = context.attribute("actor");

        if (isPermitted(actor, permittedRoles, context)) {
            handler.handle(context);
        } else {
            context.status(401).result("");
        }
    }

    private boolean isPermitted(final Object actor, final Set<Role> permittedRoles,
                                final Context context) {
        if (actor instanceof AccountBO) {
            return isPermitted((AccountBO) actor, permittedRoles);
        } else if (actor instanceof ClientBO) {
            return isPermitted((ClientBO) actor, permittedRoles);
        }

        final String[] pathParts = context.path().split("/");

        return unprotectedPaths.contains(pathParts[1]);
    }

    private boolean isPermitted(final AccountBO actor, final Set<Role> permittedRoles) {
        if (actor == null) {
            return false;
        }

        final Optional<ActorRole> matchedRole = actor.getRoles().stream()
                .map(ActorRole::of)
                .filter(permittedRoles::contains)
                .findFirst(); // we only need to have one valid role to access the endpoint

        return matchedRole.isPresent();
    }

    private boolean isPermitted(final ClientBO actor, final Set<Role> permittedRoles) {
        if (actor == null) {
            return false;
        }

        if (actor.getClientType() == Client.ClientType.AUTH) {
            return permittedRoles.contains(ActorRole.of(AuthGuardRoles.AUTH_CLIENT));
        }

        if (actor.getClientType() == Client.ClientType.ADMIN) {
            return permittedRoles.contains(ActorRole.of(AuthGuardRoles.ADMIN_CLIENT));
        }

        LOG.error("Undefined client type " + actor.getClientType());

        return false;
    }
}
