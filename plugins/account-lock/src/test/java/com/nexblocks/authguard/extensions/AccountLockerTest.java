package com.nexblocks.authguard.extensions;

import com.nexblocks.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.nexblocks.authguard.dal.model.ExchangeAttemptDO;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.extensions.config.ImmutableAccountLockerConfig;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.messaging.AuthMessage;
import com.nexblocks.authguard.service.model.AccountLockBO;
import com.nexblocks.authguard.service.model.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLockerTest {
    private ExchangeAttemptsRepository exchangeAttemptsRepository;
    private AccountLocksService accountLocksService;
    private ImmutableAccountLockerConfig config;

    private AccountLocker accountLocker;

    @BeforeEach
    void setup() {
        exchangeAttemptsRepository = Mockito.mock(ExchangeAttemptsRepository.class);
        accountLocksService = Mockito.mock(AccountLocksService.class);
        config = ImmutableAccountLockerConfig.builder()
                .build();

        accountLocker = new AccountLocker(exchangeAttemptsRepository, accountLocksService, config);
    }

    @Test
    void onMessageNoLock() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.ACCOUNT, 101L);

        final Message<Object> message = Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .bodyType(AuthMessage.class)
                .messageBody(authMessage)
                .timestamp(Instant.now())
                .build();

        // mocks
        Mockito.when(exchangeAttemptsRepository.findByEntityAndTimestamp(Mockito.anyLong(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        // call
        accountLocker.onMessage(message);

        // verify
        final ArgumentCaptor<Instant> timeArgumentCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(exchangeAttemptsRepository)
                .findByEntityAndTimestamp(Mockito.eq(101L), timeArgumentCaptor.capture());

        assertThat(timeArgumentCaptor.getValue()).isBetween(
                Instant.now()
                        .minus(Duration.ofMinutes(config.getCheckPeriod()))
                        .minus(Duration.ofMinutes(1)),
                Instant.now()
                        .minus(Duration.ofMinutes(config.getCheckPeriod()))
                        .plus(Duration.ofMinutes(1))
        );

        Mockito.verifyZeroInteractions(accountLocksService);
    }

    @Test
    void onMessageLock() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.ACCOUNT, 101L);

        final Message<Object> message = Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .bodyType(AuthMessage.class)
                .messageBody(authMessage)
                .timestamp(Instant.now())
                .build();

        // mocks
        Mockito.when(exchangeAttemptsRepository.findByEntityAndTimestamp(Mockito.anyLong(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(
                        ExchangeAttemptDO.builder().build(),
                        ExchangeAttemptDO.builder().build(),
                        ExchangeAttemptDO.builder().build()
                )));

        // call
        accountLocker.onMessage(message);

        // verify
        final ArgumentCaptor<Instant> timeArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
        final ArgumentCaptor<AccountLockBO> accountLockArgumentCaptor = ArgumentCaptor.forClass(AccountLockBO.class);

        Mockito.verify(exchangeAttemptsRepository)
                .findByEntityAndTimestamp(Mockito.eq(101L), timeArgumentCaptor.capture());

        assertThat(timeArgumentCaptor.getValue()).isBetween(
                Instant.now()
                        .minus(Duration.ofMinutes(config.getCheckPeriod()))
                        .minus(Duration.ofMinutes(1)),
                Instant.now()
                        .minus(Duration.ofMinutes(config.getCheckPeriod()))
                        .plus(Duration.ofMinutes(1))
        );

        Mockito.verify(accountLocksService).create(accountLockArgumentCaptor.capture());

        assertThat(accountLockArgumentCaptor.getValue().getAccountId()).isEqualTo(authMessage.getEntityId());
        assertThat(accountLockArgumentCaptor.getValue().getExpiresAt()).isBetween(
                Instant.now()
                        .plus(Duration.ofMinutes(config.getLockPeriod()))
                        .minus(Duration.ofMinutes(1)),
                Instant.now()
                        .plus(Duration.ofMinutes(config.getLockPeriod()))
                        .plus(Duration.ofMinutes(1))
        );
    }

    @Test
    void onMessageNotAuth() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.ACCOUNT, 101L);

        final Message<Object> message = Message.builder()
                .eventType(EventType.EMAIL_VERIFICATION)
                .bodyType(AuthMessage.class)
                .messageBody(authMessage)
                .timestamp(Instant.now())
                .build();

        // call
        accountLocker.onMessage(message);

        // verify
        Mockito.verifyZeroInteractions(exchangeAttemptsRepository, accountLocksService);
    }

    @Test
    void onMessageAuthWrongBodyType() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.ACCOUNT, 101L);

        final Message<Object> message = Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .bodyType(ImmutableAccountLockerConfig.class)
                .messageBody(authMessage)
                .timestamp(Instant.now())
                .build();

        // call
        accountLocker.onMessage(message);

        // verify
        Mockito.verifyZeroInteractions(exchangeAttemptsRepository, accountLocksService);
    }

    @Test
    void onMessageAuthNotAccount() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.APPLICATION, 101L);

        final Message<Object> message = Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .bodyType(AuthMessage.class)
                .messageBody(authMessage)
                .timestamp(Instant.now())
                .build();

        // call
        accountLocker.onMessage(message);

        // verify
        Mockito.verifyZeroInteractions(exchangeAttemptsRepository, accountLocksService);
    }
}