package com.nexblocks.authguard.api.access;

import io.javalin.core.security.Role;

import java.util.Set;

import static io.javalin.core.security.SecurityUtil.roles;

public class ActorRoles {
    public static Set<Role> of(String... roles) {
        final ActorRole[] actorRoles = new ActorRole[roles.length];

        for (int i = 0; i < roles.length; i++) {
            actorRoles[i] = ActorRole.of(roles[i]);
        }

        return roles(actorRoles);
    }

    public static Set<Role> adminClient() {
        return of(AuthGuardRoles.ADMIN_CLIENT);
    }

    public static Set<Role> adminAccount() {
        return of(AuthGuardRoles.ADMIN_ACCOUNT);
    }

    public static Set<Role> anyAdmin() {
        return of(AuthGuardRoles.ADMIN_ACCOUNT, AuthGuardRoles.ADMIN_CLIENT);
    }

    public static Set<Role> anyAdminOrAuthClient() {
        return of(AuthGuardRoles.ADMIN_ACCOUNT, AuthGuardRoles.ADMIN_CLIENT, AuthGuardRoles.AUTH_CLIENT);
    }

    public static Set<Role> adminOrAuthClient() {
        return of(AuthGuardRoles.ADMIN_CLIENT, AuthGuardRoles.AUTH_CLIENT);
    }
}
