package com.authguard.dal.persistence;

import com.authguard.dal.model.PermissionDO;
import com.authguard.dal.repository.ImmutableRecordRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PermissionsRepository extends ImmutableRecordRepository<PermissionDO> {
    CompletableFuture<Optional<PermissionDO>> search(String group, String name);
    CompletableFuture<Collection<PermissionDO>> getAll();
    CompletableFuture<Collection<PermissionDO>> getAllForGroup(String group);
}
