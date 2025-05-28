package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ClientBO;

import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface ClientsService extends IdempotentCrudService<ClientBO> {
    Uni<Optional<ClientBO>> getByIdUnchecked(long id);
    Uni<Optional<ClientBO>> getByExternalId(String externalId, String domain);
    Uni<ClientBO> activate(long id, String domain);
    Uni<ClientBO> deactivate(long id, String domain);
    Uni<List<ClientBO>> getByAccountId(long accountId, String domain, Long cursor);
    Uni<List<ClientBO>> getByDomain(String domain, Long cursor);
}
