package org.auther.service.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface Account {
    String getId();
    List<PermissionBO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    boolean isActive();
    boolean isDeleted();
}
