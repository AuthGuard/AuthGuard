package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.PermissionDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PermissionsRepository extends ImmutableRecordRepository<PermissionDO> {
    CompletableFuture<Optional<PermissionDO>> search(String group, String name, String domain);
    CompletableFuture<Collection<PermissionDO>> getAll(String domain);
    CompletableFuture<Collection<PermissionDO>> getAllForGroup(String group, String domain);
}
