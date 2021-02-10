package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.ExchangeAttemptDO;
import com.nexblocks.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ExchangeAttemptsService;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.ExchangeAttemptBO;
import com.nexblocks.authguard.service.model.ExchangeAttemptsQueryBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeAttemptsServiceImplTest {
    private ExchangeAttemptsRepository repository;
    private ExchangeAttemptsService service;
    private MessageBus messageBus;

    @BeforeEach
    void setup() {
        repository = Mockito.mock(ExchangeAttemptsRepository.class);
        messageBus = Mockito.mock(MessageBus.class);

        service = new ExchangeAttemptsServiceImpl(repository, new ServiceMapperImpl(), messageBus);
    }

    @Test
    void findByEntityId() {
        final ExchangeAttemptsQueryBO query = ExchangeAttemptsQueryBO.builder()
                .entityId("account")
                .build();

        Mockito.when(repository.findByEntity("account"))
                .thenReturn(CompletableFuture.completedFuture(
                        Collections.singletonList(ExchangeAttemptDO.builder()
                                .entityId("account")
                                .exchangeFrom("basic")
                                .exchangeTo("session")
                                .build())
                ));

        final Collection<ExchangeAttemptBO> actual = service.find(query);
        final Collection<ExchangeAttemptBO> expected = Collections.singletonList(ExchangeAttemptBO.builder()
                .entityId("account")
                .exchangeFrom("basic")
                .exchangeTo("session")
                .build());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void findByEntityIdAndFromTimestamp() {
        final OffsetDateTime now = OffsetDateTime.now();
        final ExchangeAttemptsQueryBO query = ExchangeAttemptsQueryBO.builder()
                .entityId("account")
                .fromTimestamp(now)
                .build();

        Mockito.when(repository.findByEntityAndTimestamp("account", now))
                .thenReturn(CompletableFuture.completedFuture(
                        Collections.singletonList(ExchangeAttemptDO.builder()
                                .entityId("account")
                                .exchangeFrom("basic")
                                .exchangeTo("session")
                                .build())
                ));

        final Collection<ExchangeAttemptBO> actual = service.find(query);
        final Collection<ExchangeAttemptBO> expected = Collections.singletonList(ExchangeAttemptBO.builder()
                .entityId("account")
                .exchangeFrom("basic")
                .exchangeTo("session")
                .build());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void findByEntityIdAndFromTimestampAndExchange() {
        final OffsetDateTime now = OffsetDateTime.now();
        final ExchangeAttemptsQueryBO query = ExchangeAttemptsQueryBO.builder()
                .entityId("account")
                .fromTimestamp(now)
                .fromExchange("basic")
                .build();

        Mockito.when(repository.findByEntityAndTimestampAndExchange("account", now, "basic"))
                .thenReturn(CompletableFuture.completedFuture(
                        Collections.singletonList(ExchangeAttemptDO.builder()
                                .entityId("account")
                                .exchangeFrom("basic")
                                .exchangeTo("session")
                                .build())
                ));

        final Collection<ExchangeAttemptBO> actual = service.find(query);
        final Collection<ExchangeAttemptBO> expected = Collections.singletonList(ExchangeAttemptBO.builder()
                .entityId("account")
                .exchangeFrom("basic")
                .exchangeTo("session")
                .build());

        assertThat(actual).isEqualTo(expected);
    }
}