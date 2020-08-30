package com.authguard.dal;

import com.authguard.dal.common.ImmutableRecordRepository;
import com.authguard.dal.model.RoleDO;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RolesRepository extends ImmutableRecordRepository<RoleDO> {
    CompletableFuture<Collection<RoleDO>> getAll();
    CompletableFuture<Optional<RoleDO>> getByName(String name);
}
