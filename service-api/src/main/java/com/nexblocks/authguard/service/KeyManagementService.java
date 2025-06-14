package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.EphemeralKeyBO;
import com.nexblocks.authguard.service.model.PersistedKeyBO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface KeyManagementService extends CrudService<PersistedKeyBO> {
    EphemeralKeyBO generate(String algorithm, int size);
    Uni<Optional<PersistedKeyBO>> getDecrypted(long id, String domain, String passcode);
    Uni<List<PersistedKeyBO>> getByDomain(String domain, Instant cursor);
    Uni<List<PersistedKeyBO>> getByAccountId(String domain, long accountId, Instant cursor);
    Uni<List<PersistedKeyBO>> getByAppId(String domain, long appId, Instant cursor);
}
