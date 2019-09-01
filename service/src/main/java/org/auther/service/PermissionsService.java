package org.auther.service;

import org.auther.service.model.PermissionBO;
import org.auther.service.model.PermissionGroupBO;

import java.util.List;
import java.util.Optional;

public interface PermissionsService {
    PermissionGroupBO createPermissionGroup(PermissionGroupBO permissionGroup);
    PermissionBO createPermission(PermissionBO permission);
    List<PermissionBO> verifyPermissions(List<PermissionBO> permissions);
    List<PermissionBO> getPermissions();
    Optional<List<PermissionBO>> getPermissionsByGroup(String group);
    Optional<PermissionBO> deletePermission(PermissionBO permission);
}
