package com.authguard.sessions;

import com.authguard.service.SessionsService;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.model.SessionBO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                .expiresAt(ZonedDateTime.now().plus(Duration.ofMinutes(20)))
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(Optional.of(session));

        final Optional<String> accountId = sessionVerifier.verifyAccountToken(session.getSessionToken());

        assertThat(accountId).contains(session.getAccountId());
    }

    @Test
    void verifyNonExistingSession() {
        final SessionsService sessionsService = Mockito.mock(SessionsService.class);
        final SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        Mockito.when(sessionsService.getByToken(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionVerifier.verifyAccountToken("invalid"))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void verifyExpiredSession() {
        final SessionsService sessionsService = Mockito.mock(SessionsService.class);
        final SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        final SessionBO session = SessionBO.builder()
                .id("session-id")
                .sessionToken("token")
                .accountId("account-id")
                .expiresAt(ZonedDateTime.now().minus(Duration.ofMinutes(20)))
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionVerifier.verifyAccountToken("session-id"))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}