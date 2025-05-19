package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.SessionDO;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SessionsRepository {
    Uni<SessionDO> save(SessionDO session);
    Uni<Optional<SessionDO>> getById(long sessionId);
    CompletableFuture<Optional<SessionDO>> getByToken(String sessionToken);
    CompletableFuture<Optional<SessionDO>> deleteByToken(String sessionToken);
    CompletableFuture<List<SessionDO>> findByAccountId(long accountId, String domain);
}
