package com.authguard.sessions.exchange;

import com.authguard.service.SessionsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.exchange.Exchange;
import com.authguard.service.exchange.TokenExchange;
import com.authguard.service.model.AuthRequestBO;
import com.authguard.service.model.EntityType;
import com.authguard.service.model.SessionBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.vavr.control.Either;

import java.time.ZonedDateTime;
import java.util.Optional;

@TokenExchange(from = "sessionToken", to = "session")
public class SessionTokenToSession implements Exchange {
    private final SessionsService sessionsService;

    @Inject
    public SessionTokenToSession(final SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    @Override
    public Either<Exception, TokensBO> exchange(final AuthRequestBO request) {
        final Optional<SessionBO> sessionOpt = sessionsService.getByToken(request.getToken());

        if (sessionOpt.isEmpty()) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Session token does not exist"));
        }

        final SessionBO session = sessionOpt.get();

        if (session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session token has expired"));
        }

        return Either.right(TokensBO.builder()
                .type("session")
                .token(session)
                .entityType(EntityType.ACCOUNT)
                .entityId(session.getAccountId())
                .build());
    }
}
