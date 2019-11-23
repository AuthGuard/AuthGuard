package org.auther.service;

import org.auther.service.model.PermissionBO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Permissions service interface.
 *
 * @see org.auther.service.impl.PermissionsServiceImpl
 */
public interface PermissionsService {
    /**
     * Create a new permission.
     * @param permission The permissions.
     * @return The created permission.
     */
    PermissionBO create(PermissionBO permission);

    /**
     * Verify that permissions exist.
     * @return A list containing only valid permissions.
     */
    List<PermissionBO> validate(List<PermissionBO> permissions);

    /**
     * @return A list containing all created permissions.
     */
    List<PermissionBO> getAll();

    Collection<PermissionBO> getAllForGroup(String group);

    /**
     * Delete a permissions.
     * @return An optional of the deleted permission or empty if
     *         the permission did not exist.
     */
    Optional<PermissionBO> delete(String id);
}
