package com.authguard.service;

import com.authguard.service.model.PermissionBO;

import java.util.Collection;
import java.util.List;

/**
 * Permissions service interface.
 */
public interface PermissionsService extends CrudService<PermissionBO> {

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
}
