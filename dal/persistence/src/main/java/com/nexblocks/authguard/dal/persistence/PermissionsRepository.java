package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;

import java.util.Collection;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface PermissionsRepository extends ImmutableRecordRepository<PermissionDO> {
    Uni<Optional<PermissionDO>> search(String group, String name, String domain);
    Uni<Collection<PermissionDO>> getAll(String domain, Page<Long> page);
    Uni<Collection<PermissionDO>> getAllForGroup(String group, String domain, Page<Long> page);
}
