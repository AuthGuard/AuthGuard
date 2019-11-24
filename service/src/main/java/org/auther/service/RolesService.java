package org.auther.service;

import org.auther.service.model.PermissionBO;
import org.auther.service.model.RoleBO;

import java.util.List;
import java.util.Optional;

/**
 * Roles service interface.
 *
 * @see org.auther.service.impl.RolesServiceImpl
 */
public interface RolesService {
    /**
     * @return All created roles.
     */
    List<RoleBO> getAll();

    /**
     * Create a role.
     * @param role The role.
     * @return The created role.
     */
    RoleBO create(RoleBO role);

    /**
     * Find a role by name.
     * @param name The name of the role.
     * @return Optional of the found role or empty
     *         if none was found.
     */
    Optional<RoleBO> getRoleByName(String name);

    /**
     * Find permissions of a role by name.
     * @param name The name of the role.
     * @return A list of permissions granted to that role.
     * @throws org.auther.service.exceptions.ServiceNotFoundException
     *         if none was found.
     */
    List<PermissionBO> getPermissionsByName(String name);

    /**
     * Grant permissions to a role.
     * @param name The name of the role.
     * @param permission A list of permissions to grant.
     * @return Optional of the updated role or empty if none
     *         was found.
     */
    Optional<RoleBO> grantPermissions(String name, List<PermissionBO> permission);

    /**
     * Revoke permissions of a role.
     * @param name The name of the role.
     * @param permissions A list of permissions to grant.
     * @return Optional of the updated role or empty if none
     *         was found.
     */
    Optional<RoleBO> revokePermissions(String name, List<PermissionBO> permissions);
}
