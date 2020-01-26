package com.authguard.dal.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
public interface App extends AbstractDO {
    String getName();
    String getParentAccountId();
    List<String> getRoles();
    List<PermissionDO> getPermissions();
    List<String> getScopes();
    boolean isActive();

    interface Builder extends AbstractDO.Builder {}
}
