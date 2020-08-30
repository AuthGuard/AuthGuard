package com.authguard.dal;

import com.authguard.dal.common.ImmutableRecordRepository;
import com.authguard.dal.model.PermissionDO;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PermissionsRepository extends ImmutableRecordRepository<PermissionDO> {
    CompletableFuture<Optional<PermissionDO>> search(String group, String name);
    CompletableFuture<Collection<PermissionDO>> getAll();
    CompletableFuture<Collection<PermissionDO>> getAllForGroup(String group);
}
