package com.nexblocks.authguard.sessions.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.SessionBO;
import com.nexblocks.authguard.service.model.TokensBO;
import io.vavr.control.Either;

import java.time.OffsetDateTime;
import java.util.Optional;

@TokenExchange(from = "sessionToken", to = "session")
public class SessionTokenToSession implements Exchange {
    private static final String TOKEN_TYPE = "session";

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

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return Either.left(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session token has expired"));
        }

        return Either.right(TokensBO.builder()
                .type(TOKEN_TYPE)
                .token(session)
                .entityType(EntityType.ACCOUNT)
                .entityId(session.getAccountId())
                .build());
    }
}
