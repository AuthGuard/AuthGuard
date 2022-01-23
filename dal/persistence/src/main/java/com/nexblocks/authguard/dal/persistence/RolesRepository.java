package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.RoleDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RolesRepository extends ImmutableRecordRepository<RoleDO> {
    CompletableFuture<Collection<RoleDO>> getAll(String domain);
    CompletableFuture<Optional<RoleDO>> getByName(String name, String domain);
    CompletableFuture<Collection<RoleDO>> getMultiple(Collection<String> rolesNames, String domain);
}
