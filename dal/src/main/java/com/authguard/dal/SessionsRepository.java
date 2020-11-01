package com.authguard.dal;

import com.authguard.dal.model.SessionDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SessionsRepository {
    CompletableFuture<SessionDO> save(final SessionDO session);

    CompletableFuture<Optional<SessionDO>> getById(final String sessionId);

    CompletableFuture<Optional<SessionDO>> getByToken(final String sessionToken);
}
