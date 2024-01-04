package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.ClientBO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ClientsService extends IdempotentCrudService<ClientBO> {
    CompletableFuture<Optional<ClientBO>> getByExternalId(String externalId);
    CompletableFuture<ClientBO> activate(long id);
    CompletableFuture<ClientBO> deactivate(long id);
    CompletableFuture<List<ClientBO>> getByAccountId(long accountId);
}
