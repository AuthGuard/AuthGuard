package com.nexblocks.authguard.sessions;

import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.SessionBO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class SessionVerifierTest {

    @Test
    void verify() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);
        SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        SessionBO session = SessionBO.builder()
                .id(1)
                .sessionToken("token")
                .accountId(101)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(20)))
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(session)));

        Long accountId = sessionVerifier.verifyAccountTokenAsync(session.getSessionToken()).join();

        assertThat(accountId).isEqualTo(session.getAccountId());
    }

    @Test
    void verifyNonExistingSession() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);
        SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        Mockito.when(sessionsService.getByToken(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> sessionVerifier.verifyAccountTokenAsync("invalid").join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void verifyExpiredSession() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);
        SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        SessionBO session = SessionBO.builder()
                .id(1)
                .sessionToken("token")
                .accountId(101)
                .expiresAt(Instant.now().minus(Duration.ofMinutes(20)))
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(session)));

        assertThatThrownBy(() -> sessionVerifier.verifyAccountTokenAsync(session.getSessionToken()).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }
}