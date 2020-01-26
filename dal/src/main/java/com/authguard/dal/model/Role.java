package com.authguard.dal.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
public interface Role extends AbstractDO {
    String getName();
    List<PermissionDO> getPermissions();

    interface Builder extends AbstractDO.Builder {}
}
