package org.auther.service.model;

import org.jetbrains.annotations.Nullable;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface Account {
    @Nullable String getId();
    String getUsername();
    @Nullable String getPlainPassword();
    @Nullable HashedPasswordBO getHashedPassword();
    List<PermissionBO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    boolean isActive();
    boolean isDeleted();
}
