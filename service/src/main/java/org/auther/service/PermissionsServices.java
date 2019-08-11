package org.auther.service;

import org.auther.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;

public interface PermissionsServices {
    PermissionGroupBO createPermissionGroup(PermissionGroupBO permissionGroup);
    PermissionBO createPermission(PermissionBO permission);
    List<PermissionBO> getPermissions();
    Optional<List<PermissionBO>> getPermissionsByGroup(String group);
    Optional<PermissionBO> deletePermission(PermissionBO permission);
}
