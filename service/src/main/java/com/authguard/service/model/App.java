package com.authguard.service.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface App {
    String getId();
    String getExternalId();
    String getName();
    String getParentAccountId();
    List<PermissionBO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    boolean isActive();
    boolean isDeleted();
}
