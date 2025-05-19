package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.SessionDO;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface SessionsRepository {
    Uni<SessionDO> save(SessionDO session);
    Uni<Optional<SessionDO>> getById(long sessionId);
    Uni<Optional<SessionDO>> getByToken(String sessionToken);
    Uni<Optional<SessionDO>> deleteByToken(String sessionToken);
    Uni<List<SessionDO>> findByAccountId(long accountId, String domain);
}
