package org.auther.service;

import org.auther.service.model.PermissionBO;
import org.auther.service.model.PermissionGroupBO;

import java.util.List;
import java.util.Optional;

/**
 * Permissions service interface.
 *
 * @see org.auther.service.impl.PermissionsServiceImpl
 */
public interface PermissionsService {
    /**
     * Create a new permissions group.
     * @param permissionGroup The permissions group.
     * @return The created group.
     */
    PermissionGroupBO createPermissionGroup(PermissionGroupBO permissionGroup);

    /**
     * Create a new permission.
     * @param permission The permissions.
     * @return The created permission.
     */
    PermissionBO createPermission(PermissionBO permission);

    /**
     * Verify that permissions exist.
     * @param permissions The permissions to check.
     * @return A list containing only valid permissions.
     */
    List<PermissionBO> verifyPermissions(List<PermissionBO> permissions);

    /**
     * @return A list containing all created permissions.
     */
    List<PermissionBO> getPermissions();

    /**
     * Find all permissions by their group.
     * @param group The group name.
     * @return An optional of all permissions under that that group
     *         or empty if the group is not found.
     */
    Optional<List<PermissionBO>> getPermissionsByGroup(String group);

    /**
     * Delete a permissions.
     * @param permission The permission.
     * @return An optional of the deleted permission or empty if
     *         the permission did not exist.
     */
    Optional<PermissionBO> deletePermission(PermissionBO permission);
}
