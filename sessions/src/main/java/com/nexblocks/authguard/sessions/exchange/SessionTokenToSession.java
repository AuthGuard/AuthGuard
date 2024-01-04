package com.nexblocks.authguard.sessions.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.SessionBO;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@TokenExchange(from = "sessionToken", to = "session")
public class SessionTokenToSession implements Exchange {
    private static final String TOKEN_TYPE = "session";

    private final SessionsService sessionsService;

    @Inject
    public SessionTokenToSession(final SessionsService sessionsService) {
        this.sessionsService = sessionsService;
    }

    @Override
    public CompletableFuture<AuthResponseBO> exchange(final AuthRequestBO request) {
        return sessionsService.getByToken(request.getToken())
                .thenCompose(sessionOpt -> {
                    if (sessionOpt.isEmpty()) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.INVALID_TOKEN, "Session token does not exist"));
                    }

                    SessionBO session = sessionOpt.get();

                    if (session.getExpiresAt().isBefore(Instant.now())) {
                        return CompletableFuture.failedFuture(new ServiceAuthorizationException(ErrorCode.EXPIRED_TOKEN, "Session token has expired"));
                    }

                    return CompletableFuture.completedFuture(AuthResponseBO.builder()
                            .type(TOKEN_TYPE)
                            .token(session)
                            .entityType(EntityType.ACCOUNT)
                            .entityId(session.getAccountId())
                            .build());
                });
    }
}
