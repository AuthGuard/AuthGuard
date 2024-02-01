package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AppBO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApplicationsService extends IdempotentCrudService<AppBO> {
    CompletableFuture<Optional<AppBO>> getByExternalId(long externalId, String domain);
    CompletableFuture<AppBO> activate(long id, String domain);
    CompletableFuture<AppBO> deactivate(long id, String domain);
    CompletableFuture<List<AppBO>> getByAccountId(long accountId, String domain, Long cursor);
}
