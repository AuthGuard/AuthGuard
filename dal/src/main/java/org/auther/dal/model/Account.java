package org.auther.dal.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
public interface Account {
    String getId();
    String getUsername();
    HashedPasswordDO getHashedPassword();
    List<String> getRoles();
    List<PermissionDO> getPermissions();
    List<String> getScopes();
    boolean isActive();
    boolean isDeleted();
}
