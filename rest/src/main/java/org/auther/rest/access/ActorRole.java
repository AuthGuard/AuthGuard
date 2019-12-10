package org.auther.rest.access;

import io.javalin.core.security.Role;

public class ActorRole implements Role {
    private final String role;

    public ActorRole(final String role) {
        this.role = role;
    }

    public static ActorRole of(final String role) {
        return new ActorRole(role);
    }
}
