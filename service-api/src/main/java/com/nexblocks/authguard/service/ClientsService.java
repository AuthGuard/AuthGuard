package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ClientBO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ClientsService extends IdempotentCrudService<ClientBO> {
    CompletableFuture<Optional<ClientBO>> getByIdUnchecked(long id);
    CompletableFuture<Optional<ClientBO>> getByExternalId(String externalId, String domain);
    CompletableFuture<ClientBO> activate(long id, String domain);
    CompletableFuture<ClientBO> deactivate(long id, String domain);
    CompletableFuture<List<ClientBO>> getByAccountId(long accountId, String domain);
}
