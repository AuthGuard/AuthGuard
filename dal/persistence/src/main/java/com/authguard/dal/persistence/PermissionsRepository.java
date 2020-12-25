package com.authguard.dal.persistence;

import com.authguard.dal.model.PermissionDO;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PermissionsRepository {
    CompletableFuture<PermissionDO> save(PermissionDO permission);
    CompletableFuture<Optional<PermissionDO>> getById(String id);
    CompletableFuture<Optional<PermissionDO>> search(String group, String name);
    CompletableFuture<Optional<PermissionDO>> delete(String id);
    CompletableFuture<Collection<PermissionDO>> getAll();
    CompletableFuture<Collection<PermissionDO>> getAllForGroup(String group);
}
