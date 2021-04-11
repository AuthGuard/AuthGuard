package com.nexblocks.authguard.sessions;

import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.SessionBO;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class SessionVerifierTest {

    @Test
    void verify() {
        final SessionsService sessionsService = Mockito.mock(SessionsService.class);
        final SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        final SessionBO session = SessionBO.builder()
                .id("session-id")
                .sessionToken("token")
                .accountId("account-id")
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMinutes(20)))
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(Optional.of(session));

        final Either<Exception, String> accountId = sessionVerifier.verifyAccountToken(session.getSessionToken());

        assertThat(accountId.get()).isEqualTo(session.getAccountId());
    }

    @Test
    void verifyNonExistingSession() {
        final SessionsService sessionsService = Mockito.mock(SessionsService.class);
        final SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        Mockito.when(sessionsService.getByToken(any()))
                .thenReturn(Optional.empty());

        final Either<Exception, String> result = sessionVerifier.verifyAccountToken("invalid");

        assertThat(result.isLeft());
        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void verifyExpiredSession() {
        final SessionsService sessionsService = Mockito.mock(SessionsService.class);
        final SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        final SessionBO session = SessionBO.builder()
                .id("session-id")
                .sessionToken("token")
                .accountId("account-id")
                .expiresAt(OffsetDateTime.now().minus(Duration.ofMinutes(20)))
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(Optional.of(session));

        final Either<Exception, String> result = sessionVerifier.verifyAccountToken("session-id");

        assertThat(result.isLeft());
        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }
}