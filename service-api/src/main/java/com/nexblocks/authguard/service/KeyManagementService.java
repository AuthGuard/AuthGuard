package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.EphemeralKeyBO;
import com.nexblocks.authguard.service.model.PersistedKeyBO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface KeyManagementService extends CrudService<PersistedKeyBO> {
    EphemeralKeyBO generate(String algorithm, int size);
    CompletableFuture<Optional<PersistedKeyBO>> getDecrypted(long id, String domain, String passcode);
    CompletableFuture<List<PersistedKeyBO>> getByAccountId(long accountId, Instant cursor);
    CompletableFuture<List<PersistedKeyBO>> getByAppId(long appId, Instant cursor);
}
