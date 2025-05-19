package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.RoleBO;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RolesService extends CrudService<RoleBO> {
    CompletableFuture<List<RoleBO>> getAll(String domain, Long cursor);
    CompletableFuture<Optional<RoleBO>> getRoleByName(String name, String domain);
    // FIXME use Uni for this
    Uni<List<String>> verifyRoles(Collection<String> roles, String domain, EntityType entityType);
}
