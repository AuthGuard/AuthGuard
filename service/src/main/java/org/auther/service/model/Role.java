package org.auther.service.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface Role {
    String getName();
    List<PermissionBO> getPermissions();
}
