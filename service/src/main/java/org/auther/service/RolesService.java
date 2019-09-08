package org.auther.service;

import org.auther.service.model.PermissionBO;
import org.auther.service.model.RoleBO;

import java.util.List;
import java.util.Optional;

public interface RolesService {
    List<RoleBO> getRoles();
    RoleBO createRole(RoleBO role);
    Optional<RoleBO> getRoleByName(String name);
    Optional<RoleBO> deleteRoleByName(String name);
    List<PermissionBO> getPermissionsByName(String name);
    Optional<RoleBO> grantPermissions(String name, List<PermissionBO> permission);
    Optional<RoleBO> revokePermissions(String name, List<PermissionBO> permissions);
}
