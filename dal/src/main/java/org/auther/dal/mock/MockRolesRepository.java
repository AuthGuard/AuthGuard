package org.auther.dal.mock;

import org.auther.dal.RolesRepository;
import org.auther.dal.model.RoleDO;

import java.util.Optional;

public class MockRolesRepository extends AbstractRepository<RoleDO> implements RolesRepository {
    @Override
    public Optional<RoleDO> getByName(final String name) {
        return getRepo().values()
                .stream()
                .filter(role -> role.getName().equals(name))
                .findFirst();
    }
}
