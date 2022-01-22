package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.RoleBO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RolesService extends CrudService<RoleBO> {
    List<RoleBO> getAll(final String domain);
    Optional<RoleBO> getRoleByName(String name, final String domain);
    List<String> verifyRoles(Collection<String> roles, final String domain);
}
