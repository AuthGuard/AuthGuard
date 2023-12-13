package com.nexblocks.authguard.sessions;

import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.auth.AuthVerifier;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.SessionBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.time.Instant;

public class SessionVerifier implements AuthVerifier {
    private final SessionsService sessionsService;

    @Inject
    public SessionVerifier(final SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    @Override
    public Either<Exception, Long> verifyAccountToken(final String sessionToken) {
        return sessionsService.getByToken(sessionToken)
                .map(this::verifySession)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session token")));
    }

    private Either<Exception, Long> verifySession(final SessionBO session) {
        if (session.getExpiresAt().isBefore(Instant.now())) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session has expired",
                    EntityType.ACCOUNT, session.getAccountId()));
        }

        return Either.right(session.getAccountId());
    }
}
