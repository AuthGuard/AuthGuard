package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.PermissionBO;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface PermissionsService extends CrudService<PermissionBO> {
    Uni<List<PermissionBO>> validate(Collection<PermissionBO> permissions, String domain, EntityType entityType);
    Uni<List<PermissionBO>> getAll(String domain, Long cursor);
    Uni<List<PermissionBO>> getAllForGroup(String group, String domain, Long cursor);
    Uni<Optional<PermissionBO>> get(String domain, String group, String name);
}
