package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.OffsetDateTime;
import java.util.Set;

@Value.Immutable
@BOStyle
public interface Credentials extends Entity {
    String getAccountId();
    Set<UserIdentifierBO> getIdentifiers();
    String getPlainPassword();
    HashedPasswordBO getHashedPassword();
    OffsetDateTime getPasswordUpdatedAt();
    Integer getPasswordVersion();
    String getDomain();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "Credentials";
    }
}
