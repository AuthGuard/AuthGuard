package com.authguard.extensions;

import com.authguard.dal.AccountLocksRepository;
import com.authguard.dal.ExchangeAttemptsRepository;
import com.authguard.dal.model.AccountLockDO;
import com.authguard.dal.model.ExchangeAttemptDO;
import com.authguard.emb.model.EventType;
import com.authguard.emb.model.Message;
import com.authguard.extensions.config.ImmutableAccountLockerConfig;
import com.authguard.service.messaging.AuthMessage;
import com.authguard.service.model.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLockerTest {
    private ExchangeAttemptsRepository exchangeAttemptsRepository;
    private AccountLocksRepository accountLocksRepository;
    private ImmutableAccountLockerConfig config;

    private AccountLocker accountLocker;

    @BeforeEach
    void setup() {
        exchangeAttemptsRepository = Mockito.mock(ExchangeAttemptsRepository.class);
        accountLocksRepository = Mockito.mock(AccountLocksRepository.class);
        config = ImmutableAccountLockerConfig.builder()
                .build();

        accountLocker = new AccountLocker(exchangeAttemptsRepository, accountLocksRepository, config);
    }

    @Test
    void onMessageNoLock() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.ACCOUNT, "account");

        final Message<Object> message = Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .bodyType(AuthMessage.class)
                .messageBody(authMessage)
                .timestamp(OffsetDateTime.now())
                .build();

        // mocks
        Mockito.when(exchangeAttemptsRepository.findByEntityAndTimestamp(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        // call
        accountLocker.onMessage(message);

        // verify
        final ArgumentCaptor<OffsetDateTime> timeArgumentCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        Mockito.verify(exchangeAttemptsRepository)
                .findByEntityAndTimestamp(Mockito.eq("account"), timeArgumentCaptor.capture());

        assertThat(timeArgumentCaptor.getValue()).isBetween(
                OffsetDateTime.now().minusMinutes(config.getCheckPeriod()).minusMinutes(1),
                OffsetDateTime.now().minusMinutes(config.getCheckPeriod()).plusMinutes(1)
        );

        Mockito.verifyZeroInteractions(accountLocksRepository);
    }

    @Test
    void onMessageLock() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.ACCOUNT, "account");

        final Message<Object> message = Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .bodyType(AuthMessage.class)
                .messageBody(authMessage)
                .timestamp(OffsetDateTime.now())
                .build();

        // mocks
        Mockito.when(exchangeAttemptsRepository.findByEntityAndTimestamp(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(
                        ExchangeAttemptDO.builder().build(),
                        ExchangeAttemptDO.builder().build(),
                        ExchangeAttemptDO.builder().build()
                )));

        // call
        accountLocker.onMessage(message);

        // verify
        final ArgumentCaptor<OffsetDateTime> timeArgumentCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        final ArgumentCaptor<AccountLockDO> accountLockArgumentCaptor = ArgumentCaptor.forClass(AccountLockDO.class);

        Mockito.verify(exchangeAttemptsRepository)
                .findByEntityAndTimestamp(Mockito.eq("account"), timeArgumentCaptor.capture());

        assertThat(timeArgumentCaptor.getValue()).isBetween(
                OffsetDateTime.now().minusMinutes(config.getCheckPeriod()).minusMinutes(1),
                OffsetDateTime.now().minusMinutes(config.getCheckPeriod()).plusMinutes(1)
        );

        Mockito.verify(accountLocksRepository).save(accountLockArgumentCaptor.capture());

        assertThat(accountLockArgumentCaptor.getValue().getAccountId()).isEqualTo(authMessage.getEntityId());
        assertThat(accountLockArgumentCaptor.getValue().getExpiresAt()).isBetween(
                OffsetDateTime.now().plusMinutes(config.getLockPeriod()).minusMinutes(1),
                OffsetDateTime.now().plusMinutes(config.getLockPeriod()).plusMinutes(1)
        );
    }

    @Test
    void onMessageNotAuth() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.ACCOUNT, "account");

        final Message<Object> message = Message.builder()
                .eventType(EventType.EMAIL_VERIFICATION)
                .bodyType(AuthMessage.class)
                .messageBody(authMessage)
                .timestamp(OffsetDateTime.now())
                .build();

        // call
        accountLocker.onMessage(message);

        // verify
        Mockito.verifyZeroInteractions(exchangeAttemptsRepository, accountLocksRepository);
    }

    @Test
    void onMessageAuthWrongBodyType() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.ACCOUNT, "account");

        final Message<Object> message = Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .bodyType(ImmutableAccountLockerConfig.class)
                .messageBody(authMessage)
                .timestamp(OffsetDateTime.now())
                .build();

        // call
        accountLocker.onMessage(message);

        // verify
        Mockito.verifyZeroInteractions(exchangeAttemptsRepository, accountLocksRepository);
    }

    @Test
    void onMessageAuthNotAccount() {
        // data
        final AuthMessage authMessage = AuthMessage.success("basic", "session",
                EntityType.APPLICATION, "account");

        final Message<Object> message = Message.builder()
                .eventType(EventType.AUTHENTICATION)
                .bodyType(AuthMessage.class)
                .messageBody(authMessage)
                .timestamp(OffsetDateTime.now())
                .build();

        // call
        accountLocker.onMessage(message);

        // verify
        Mockito.verifyZeroInteractions(exchangeAttemptsRepository, accountLocksRepository);
    }
}