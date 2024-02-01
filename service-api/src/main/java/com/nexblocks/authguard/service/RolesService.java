package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.RoleBO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RolesService extends CrudService<RoleBO> {
    CompletableFuture<List<RoleBO>> getAll(final String domain, Long cursor);
    CompletableFuture<Optional<RoleBO>> getRoleByName(String name, final String domain);
    List<String> verifyRoles(Collection<String> roles, final String domain);
}
