package com.nexblocks.authguard.sessions;

import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.AuthRequest;
import com.nexblocks.authguard.service.model.AuthRequestBO;
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

        AuthRequest request = AuthRequestBO.builder()
                .token(session.getSessionToken())
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(session)));

        Long accountId = sessionVerifier.verifyAccountTokenAsync(request).join();

        assertThat(accountId).isEqualTo(session.getAccountId());
    }

    @Test
    void verifyTrackingSession() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);
        SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        SessionBO session = SessionBO.builder()
                .id(1)
                .sessionToken("token")
                .accountId(101)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(20)))
                .forTracking(true)
                .build();

        AuthRequest request = AuthRequestBO.builder()
                .token(session.getSessionToken())
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(session)));

        assertThatThrownBy(() -> sessionVerifier.verifyAccountTokenAsync(request).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void verifyNonExistingSession() {
        SessionsService sessionsService = Mockito.mock(SessionsService.class);
        SessionVerifier sessionVerifier = new SessionVerifier(sessionsService);

        AuthRequest request = AuthRequestBO.builder()
                .token("invalid")
                .build();

        Mockito.when(sessionsService.getByToken(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> sessionVerifier.verifyAccountTokenAsync(request).join())
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

        AuthRequest request = AuthRequestBO.builder()
                .token(session.getSessionToken())
                .build();

        Mockito.when(sessionsService.getByToken(session.getSessionToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(session)));

        assertThatThrownBy(() -> sessionVerifier.verifyAccountTokenAsync(request).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }
}