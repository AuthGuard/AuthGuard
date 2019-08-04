package org.auther.dal.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
public interface Account {
    String getId();
    String getUsername();
    HashedPasswordDO getHashedPassword();
    String getRole();
    List<String> getPermissions();
    boolean isActive();
    boolean isDeleted();
}
