package org.auther.dal.mock;

import org.auther.dal.PermissionsRepository;
import org.auther.dal.model.PermissionDO;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class MockPermissionsRepository extends AbstractRepository<PermissionDO>
        implements PermissionsRepository {
    @Override
    public Optional<PermissionDO> search(final String group, final String name) {
        return getRepo().values()
                .stream()
                .filter(permission -> permission.getGroup().equals(group) && permission.getName().equals(name))
                .findFirst();
    }

    @Override
    public Collection<PermissionDO> getAllForGroup(final String group) {
        return getRepo().values()
                .stream()
                .filter(permission -> permission.getGroup().equals(group))
                .collect(Collectors.toList());
    }
}
