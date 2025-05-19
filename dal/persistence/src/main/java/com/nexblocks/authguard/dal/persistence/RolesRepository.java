package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.RoleDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;

import java.util.Collection;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface RolesRepository extends ImmutableRecordRepository<RoleDO> {
    Uni<Collection<RoleDO>> getAll(String domain, Page<Long> page);
    Uni<Optional<RoleDO>> getByName(String name, String domain);
    Uni<Collection<RoleDO>> getMultiple(Collection<String> rolesNames, String domain);
}
