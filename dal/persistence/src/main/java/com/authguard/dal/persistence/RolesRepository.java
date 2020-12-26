package com.authguard.dal.persistence;

import com.authguard.dal.model.RoleDO;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RolesRepository {
    CompletableFuture<RoleDO> save(RoleDO role);
    CompletableFuture<Optional<RoleDO>> getById(final String id);
    CompletableFuture<Collection<RoleDO>> getAll();
    CompletableFuture<Optional<RoleDO>> getByName(String name);
    CompletableFuture<Collection<RoleDO>> getMultiple(final Collection<String> rolesNames);
    CompletableFuture<Optional<RoleDO>> update(RoleDO role);
    CompletableFuture<Optional<RoleDO>> delete(String id);
}
