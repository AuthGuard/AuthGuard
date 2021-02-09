package com.nexblocks.authguard.api.access;

import io.javalin.core.security.Role;

import java.util.Objects;

public class ActorRole implements Role {
    private final String role;

    private ActorRole(final String role) {
        this.role = role;
    }

    public static ActorRole of(final String role) {
        return new ActorRole(role);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActorRole actorRole = (ActorRole) o;
        return role.equals(actorRole.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role);
    }
}
