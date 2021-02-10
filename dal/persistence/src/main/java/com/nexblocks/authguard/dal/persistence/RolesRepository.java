package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.RoleDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RolesRepository extends ImmutableRecordRepository<RoleDO> {
    CompletableFuture<Collection<RoleDO>> getAll();
    CompletableFuture<Optional<RoleDO>> getByName(String name);
    CompletableFuture<Collection<RoleDO>> getMultiple(final Collection<String> rolesNames);
}
