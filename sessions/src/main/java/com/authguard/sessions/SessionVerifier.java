package com.authguard.sessions;

import com.authguard.service.SessionsService;
import com.authguard.service.auth.AuthTokenVerfier;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.SessionBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.time.ZonedDateTime;

public class SessionVerifier implements AuthTokenVerfier {
    private final SessionsService sessionsService;

    @Inject
    public SessionVerifier(final SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    @Override
    public Either<Exception, String> verifyAccountToken(final String sessionToken) {
        return sessionsService.getByToken(sessionToken)
                .map(this::verifySession)
                .orElseGet(() -> Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session token")));
    }

    private Either<Exception, String> verifySession(final SessionBO session) {
        if (session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session has expired"));
        }

        return Either.right(session.getAccountId());
    }
}
