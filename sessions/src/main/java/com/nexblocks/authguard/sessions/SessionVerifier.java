package com.nexblocks.authguard.sessions;

import com.google.inject.Inject;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.SessionBO;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class SessionVerifier implements AuthVerifier {
    private final SessionsService sessionsService;

    @Inject
    public SessionVerifier(final SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    @Override
    public Long verifyAccountToken(final String sessionToken) {
        throw new UnsupportedOperationException("Use the async variant");
    }

    @Override
    public CompletableFuture<Long> verifyAccountTokenAsync(final String sessionToken) {
        return sessionsService.getByToken(sessionToken)
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session token"));
                    }

                    SessionBO session = opt.get();

                    if (session.getExpiresAt().isBefore(Instant.now())) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session has expired",
                                EntityType.ACCOUNT, session.getAccountId()));
                    }

                    return CompletableFuture.completedFuture(session.getAccountId());
                });
    }
}
