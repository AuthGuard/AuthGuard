package org.auther.dal.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
public interface Role {
    String getName();
    List<PermissionDO> getPermissions();
}
