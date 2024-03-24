package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.PermissionBO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PermissionsService extends CrudService<PermissionBO> {
    List<PermissionBO> validate(Collection<PermissionBO> permissions, String domain, EntityType entityType);
    CompletableFuture<List<PermissionBO>> getAll(String domain, Long cursor);
    CompletableFuture<List<PermissionBO>> getAllForGroup(String group, String domain, Long cursor);
    CompletableFuture<Optional<PermissionBO>> get(String domain, String group, String name);
}
