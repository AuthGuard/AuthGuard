package com.authguard.dal.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
public interface Account extends AbstractDO {
    List<String> getRoles();
    List<PermissionDO> getPermissions();
    List<String> getScopes();
    boolean isActive();

    interface Builder extends AbstractDO.Builder {}
}
