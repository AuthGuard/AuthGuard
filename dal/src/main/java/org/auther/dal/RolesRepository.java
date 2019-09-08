package org.auther.dal;

import org.auther.dal.model.RoleDO;

import java.util.List;
import java.util.Optional;

public interface RolesRepository {
    List<RoleDO> getAll();
    Optional<RoleDO> getByName(String name);
    RoleDO save(RoleDO role);
    Optional<RoleDO> delete(String name);
    Optional<RoleDO> update(RoleDO role);
}
