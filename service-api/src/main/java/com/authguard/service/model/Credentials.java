package com.authguard.service.model;

import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
@BOStyle
public interface Credentials extends Entity {
    String getAccountId();
    Set<UserIdentifierBO> getIdentifiers();
    String getPlainPassword();
    HashedPasswordBO getHashedPassword();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "Credentials";
    }
}
