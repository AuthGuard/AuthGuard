package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.SessionDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SessionsRepository {
    CompletableFuture<SessionDO> save(final SessionDO session);

    CompletableFuture<Optional<SessionDO>> getById(final long sessionId);

    CompletableFuture<Optional<SessionDO>> getByToken(final String sessionToken);

    CompletableFuture<Optional<SessionDO>> deleteByToken(final String sessionToken);
}
