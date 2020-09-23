package com.authguard.service.sessions;

import com.authguard.dal.SessionsRepository;
import com.authguard.service.auth.AuthTokenVerfier;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.SessionBO;
import com.google.inject.Inject;

import java.time.ZonedDateTime;
import java.util.Optional;

public class SessionVerifier implements AuthTokenVerfier {
    private final SessionsRepository sessionsRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public SessionVerifier(final SessionsRepository sessionsRepository, final ServiceMapper serviceMapper) {
        this.sessionsRepository = sessionsRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Optional<String> verifyAccountToken(final String sessionId) {
        final SessionBO session = sessionsRepository.getById(sessionId)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .orElseThrow(() -> new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Invalid session ID " + sessionId));

        if (session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session " + sessionId + " has expired");
        }

        return Optional.of(session.getAccountId());
    }
}
