package com.nexblocks.authguard.api.access;

import io.javalin.security.RouteRole;

public class ActorRoles {
    public static RouteRole[] of(String... roles) {
        final ActorRole[] actorRoles = new ActorRole[roles.length];

        for (int i = 0; i < roles.length; i++) {
            actorRoles[i] = ActorRole.of(roles[i]);
        }

        return actorRoles;
    }

    public static RouteRole[] adminClient() {
        return of(AuthGuardRoles.ADMIN_CLIENT);
    }

    public static RouteRole[] adminAccount() {
        return of(AuthGuardRoles.ADMIN_ACCOUNT);
    }

    public static RouteRole[] anyAdmin() {
        return of(AuthGuardRoles.ADMIN_ACCOUNT, AuthGuardRoles.ADMIN_CLIENT);
    }

    public static RouteRole[] anyAdminOrAuthClient() {
        return of(AuthGuardRoles.ADMIN_ACCOUNT, AuthGuardRoles.ADMIN_CLIENT, AuthGuardRoles.AUTH_CLIENT);
    }

    public static RouteRole[] adminOrAuthClient() {
        return of(AuthGuardRoles.ADMIN_CLIENT, AuthGuardRoles.AUTH_CLIENT);
    }
}
