package com.nexblocks.authguard.sessions;

import com.google.inject.Inject;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AuthRequest;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.SessionBO;

import java.time.Instant;
import io.smallrye.mutiny.Uni;

public class SessionVerifier implements AuthVerifier {
    private final SessionsService sessionsService;

    @Inject
    public SessionVerifier(final SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    @Override
    public Uni<Long> verifyAccountToken(final String sessionToken) {
        throw new UnsupportedOperationException("Use the async variant");
    }

    @Override
    public Uni<Long> verifyAccountTokenAsync(final AuthRequest request) {
        return sessionsService.getByToken(request.getToken())
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session token"));
                    }

                    SessionBO session = opt.get();

                    // tracking sessions cannot be used for auth
                    if (session.isForTracking()) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session token"));
                    }

                    if (session.getExpiresAt().isBefore(Instant.now())) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session has expired",
                                EntityType.ACCOUNT, session.getAccountId()));
                    }

                    return Uni.createFrom().item(session.getAccountId());
                });
    }
}
