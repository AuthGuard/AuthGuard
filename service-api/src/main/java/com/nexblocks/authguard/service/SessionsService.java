package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.SessionBO;

import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface SessionsService {
    Uni<SessionBO> create(SessionBO session);

    Uni<Optional<SessionBO>> getById(long id);

    Uni<Optional<SessionBO>> getByToken(String token);

    Uni<Optional<SessionBO>> deleteByToken(String token);
}
