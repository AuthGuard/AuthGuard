package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.PermissionBO;

import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface ApplicationsService extends IdempotentCrudService<AppBO> {
    Uni<Optional<AppBO>> getByExternalId(long externalId, String domain);
    Uni<AppBO> activate(long id, String domain);
    Uni<AppBO> deactivate(long id, String domain);
    Uni<List<AppBO>> getByAccountId(long accountId, String domain, Long cursor);

    Uni<Optional<AppBO>> grantPermissions(long id, List<PermissionBO> permissions, String domain);
    Uni<Optional<AppBO>> revokePermissions(long id, List<PermissionBO> permissions, String domain);
    Uni<Optional<AppBO>> grantRoles(long id, List<String> roles, String domain);
    Uni<Optional<AppBO>> revokeRoles(long id, List<String> roles, String domain);
}
