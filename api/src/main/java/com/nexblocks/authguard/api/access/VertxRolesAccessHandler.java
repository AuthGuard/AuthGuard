package com.nexblocks.authguard.api.access;

import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.ClientBO;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VertxRolesAccessHandler implements Handler<RoutingContext> {
    private static final Logger LOG = LoggerFactory.getLogger(VertxRolesAccessHandler.class);

    private final Set<String> permittedRoles;

    public VertxRolesAccessHandler(final Set<String> permittedRoles) {
        this.permittedRoles = permittedRoles;
    }

    public static VertxRolesAccessHandler forRoles(final String... actorRoles) {
        return new VertxRolesAccessHandler(Stream.of(actorRoles).collect(Collectors.toSet()));
    }

    public static VertxRolesAccessHandler onlyAdminClient() {
        return new VertxRolesAccessHandler(Stream.of(AuthGuardRoles.ADMIN_CLIENT).collect(Collectors.toSet()));
    }

    public static VertxRolesAccessHandler anyAdmin() {
        return new VertxRolesAccessHandler(Stream.of(AuthGuardRoles.ADMIN_CLIENT, AuthGuardRoles.ADMIN_ACCOUNT)
                .collect(Collectors.toSet()));
    }

    public static VertxRolesAccessHandler adminOrAuthClient() {
        return new VertxRolesAccessHandler(Stream.of(AuthGuardRoles.ADMIN_CLIENT, AuthGuardRoles.AUTH_CLIENT)
                .collect(Collectors.toSet()));
    }

    @Override
    public void handle(RoutingContext context) {
        final Object actor = context.get("actor");

        if (isPermitted(actor, context)) {
            context.next();
        } else {
            context.response()
                    .setStatusCode(403)
                    .putHeader("Content-Type", "application/json")
                    .end(io.vertx.core.json.Json.encode(new Error("403", "Access denied")));
        }
    }

    private boolean isPermitted(final Object actor, final RoutingContext context) {
        if (actor instanceof AccountBO) {
            return isPermitted((AccountBO) actor);
        } else if (actor instanceof ClientBO) {
            return isPermitted((ClientBO) actor);
        }

        return false;
    }

    private boolean isPermitted(final AccountBO actor) {
        if (actor == null) {
            return false;
        }

        return actor.getRoles().stream()
                .anyMatch(permittedRoles::contains);
    }

    private boolean isPermitted(final ClientBO actor) {
        if (actor == null) {
            return false;
        }

        switch (actor.getClientType()) {
            case AUTH:
                return permittedRoles.contains(AuthGuardRoles.AUTH_CLIENT);
            case ADMIN:
                return permittedRoles.contains(AuthGuardRoles.ADMIN_CLIENT);
            case SSO:
                return permittedRoles.contains(AuthGuardRoles.SSO_CLIENT);
            default:
                LOG.error("Undefined client type: {}", actor.getClientType());
                return false;
        }
    }
}
