package org.auther.dal;

import org.auther.dal.model.PermissionDO;
import org.auther.dal.model.PermissionGroupDO;

import java.util.List;
import java.util.Optional;

public interface PermissionsRepository {
    PermissionGroupDO createPermissionGroup(PermissionGroupDO permissionGroup);
    Optional<PermissionGroupDO> deletePermissionGroup();
    Optional<PermissionGroupDO> getPermissionGroupByName(String groupName);

    PermissionDO createPermission(PermissionDO permission);
    Optional<PermissionDO> deletePermission(PermissionDO permission);
    List<PermissionDO> getAllPermissions();
    Optional<List<PermissionDO>> getPermissions(String permissionGroup);
}
