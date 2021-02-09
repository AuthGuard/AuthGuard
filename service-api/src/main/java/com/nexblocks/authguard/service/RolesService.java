package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.RoleBO;

import java.util.List;
import java.util.Optional;

/**
 * Roles service interface.
 */
public interface RolesService extends CrudService<RoleBO> {
    /**
     * @return All created roles.
     */
    List<RoleBO> getAll();

    /**
     * Find a role by name.
     * @param name The name of the role.
     * @return Optional of the found role or empty
     *         if none was found.
     */
    Optional<RoleBO> getRoleByName(String name);

    List<String> verifyRoles(List<String> roles);
}
