package com.authguard.dal;

import com.authguard.dal.model.RoleDO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RolesRepository {
    RoleDO save(RoleDO role);
    Optional<RoleDO> getById(final String id);
    Collection<RoleDO> getAll();
    Optional<RoleDO> getByName(String name);
    Optional<RoleDO> update(RoleDO role);
    Optional<RoleDO> delete(String id);
}
