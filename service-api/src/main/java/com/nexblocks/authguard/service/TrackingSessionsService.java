package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Account;
import com.nexblocks.authguard.service.model.Session;

import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface TrackingSessionsService {
    Uni<Optional<Session>> getByToken(String token);
    Uni<Boolean> isSessionActive(String token, String domain);
    Uni<List<Session>> getByAccountId(long accountId, String domain);
    Uni<Session> startSession(Account account);
    Uni<Session> startAnonymous(String domain);
    Uni<Optional<Session>> terminateSession(String sessionToken, String domain);
}
