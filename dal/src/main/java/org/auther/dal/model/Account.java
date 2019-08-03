package org.auther.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface Account {
    String getId();
    String getUsername();
    String getPassword();
    String getRole();
    boolean isActive();
    boolean isDeleted();
}
