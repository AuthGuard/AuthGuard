package com.authguard.sessions;

import com.authguard.service.SessionsService;
import com.authguard.service.auth.AuthTokenVerfier;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.SessionBO;
import com.google.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Optional;

public class SessionVerifier implements AuthTokenVerfier {
    private final SessionsService sessionsService;

    @Inject
    public SessionVerifier(final SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    @Override
    public Optional<String> verifyAccountToken(final String sessionToken) {
        final SessionBO session = sessionsService.getByToken(sessionToken)
                .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session token"));

        if (session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session has expired");
        }

        return Optional.of(session.getAccountId());
    }
}
