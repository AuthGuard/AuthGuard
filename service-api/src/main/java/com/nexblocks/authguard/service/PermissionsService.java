package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.PermissionBO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PermissionsService extends CrudService<PermissionBO> {
    List<PermissionBO> validate(List<PermissionBO> permissions, String domain);
    CompletableFuture<List<PermissionBO>> getAll(String domain);
    CompletableFuture<List<PermissionBO>> getAllForGroup(String group, String domain);
}
