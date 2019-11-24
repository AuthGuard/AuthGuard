package org.auther.dal;

import org.auther.dal.model.PermissionDO;

import java.util.Collection;
import java.util.Optional;

public interface PermissionsRepository {
    PermissionDO save(PermissionDO permission);
    Optional<PermissionDO> getById(String id);
    Optional<PermissionDO> search(String group, String name);
    Optional<PermissionDO> delete(String id);
    Collection<PermissionDO> getAll();
    Collection<PermissionDO> getAllForGroup(String group);
}
