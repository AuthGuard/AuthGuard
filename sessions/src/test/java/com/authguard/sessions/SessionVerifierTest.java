package com.authguard.sessions;

import com.authguard.dal.SessionsRepository;
import com.authguard.dal.model.SessionDO;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.sessions.SessionVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class SessionVerifierTest {

    @Test
    void verify() {
        final SessionsRepository sessionsRepository = Mockito.mock(SessionsRepository.class);
        final SessionVerifier sessionVerifier = new SessionVerifier(sessionsRepository, new ServiceMapperImpl());

        final SessionDO session = SessionDO.builder()
                .id("session-id")
                .accountId("account-id")
                .expiresAt(ZonedDateTime.now().plus(Duration.ofMinutes(20)))
                .build();

        Mockito.when(sessionsRepository.getById(session.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(session)));

        final Optional<String> accountId = sessionVerifier.verifyAccountToken(session.getId());

        assertThat(accountId).contains(session.getAccountId());
    }

    @Test
    void verifyNonExistingSession() {
        final SessionsRepository sessionsRepository = Mockito.mock(SessionsRepository.class);
        final SessionVerifier sessionVerifier = new SessionVerifier(sessionsRepository, new ServiceMapperImpl());

        Mockito.when(sessionsRepository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> sessionVerifier.verifyAccountToken("invalid"))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void verifyExpiredSession() {
        final SessionsRepository sessionsRepository = Mockito.mock(SessionsRepository.class);
        final SessionVerifier sessionVerifier = new SessionVerifier(sessionsRepository, new ServiceMapperImpl());

        final SessionDO session = SessionDO.builder()
                .id("session-id")
                .accountId("account-id")
                .expiresAt(ZonedDateTime.now().minus(Duration.ofMinutes(20)))
                .build();

        Mockito.when(sessionsRepository.getById(session.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(session)));

        assertThatThrownBy(() -> sessionVerifier.verifyAccountToken("session-id"))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}