package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
@BOStyle
public interface App extends Entity {
    String getExternalId();
    String getName();
    String getParentAccountId();
    Set<PermissionBO> getPermissions();
    Set<String> getRoles();
    String getDomain();
    boolean isActive();
    boolean isDeleted();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "App";
    }
}
