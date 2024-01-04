package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.SessionBO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SessionsService {
    CompletableFuture<SessionBO> create(SessionBO session);

    CompletableFuture<Optional<SessionBO>> getById(long id);

    CompletableFuture<Optional<SessionBO>> getByToken(String token);

    CompletableFuture<Optional<SessionBO>> deleteByToken(String token);
}
