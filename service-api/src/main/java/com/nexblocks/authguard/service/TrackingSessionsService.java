package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Account;
import com.nexblocks.authguard.service.model.Session;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TrackingSessionsService {
    CompletableFuture<Optional<Session>> getByToken(String token);
    CompletableFuture<Boolean> isSessionActive(String token, String domain);
    CompletableFuture<List<Session>> getByAccountId(long accountId, String domain);
    CompletableFuture<Session> startSession(Account account);
    CompletableFuture<Session> startAnonymous(String domain);
    CompletableFuture<Optional<Session>> terminateSession(String sessionToken, String domain);
}
