package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.model.ExchangeAttemptDO;
import com.nexblocks.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.ExchangeAttemptsService;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.ExchangeAttemptBO;
import com.nexblocks.authguard.service.model.ExchangeAttemptsQueryBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import io.smallrye.mutiny.Uni;

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
                .entityId(101L)
                .build();

        Mockito.when(repository.findByEntity(101))
                .thenReturn(Uni.createFrom().item(
                        Collections.singletonList(ExchangeAttemptDO.builder()
                                .entityId(101L)
                                .exchangeFrom("basic")
                                .exchangeTo("session")
                                .build())
                ));

        final Collection<ExchangeAttemptBO> actual = service.find(query);
        final Collection<ExchangeAttemptBO> expected = Collections.singletonList(ExchangeAttemptBO.builder()
                .entityId(101L)
                .exchangeFrom("basic")
                .exchangeTo("session")
                .build());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void findByEntityIdAndFromTimestamp() {
        final Instant now = Instant.now();
        final ExchangeAttemptsQueryBO query = ExchangeAttemptsQueryBO.builder()
                .entityId(101L)
                .fromTimestamp(now)
                .build();

        Mockito.when(repository.findByEntityAndTimestamp(101, now))
                .thenReturn(Uni.createFrom().item(
                        Collections.singletonList(ExchangeAttemptDO.builder()
                                .entityId(101L)
                                .exchangeFrom("basic")
                                .exchangeTo("session")
                                .build())
                ));

        final Collection<ExchangeAttemptBO> actual = service.find(query);
        final Collection<ExchangeAttemptBO> expected = Collections.singletonList(ExchangeAttemptBO.builder()
                .entityId(101L)
                .exchangeFrom("basic")
                .exchangeTo("session")
                .build());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void findByEntityIdAndFromTimestampAndExchange() {
        final Instant now = Instant.now();
        final ExchangeAttemptsQueryBO query = ExchangeAttemptsQueryBO.builder()
                .entityId(101L)
                .fromTimestamp(now)
                .fromExchange("basic")
                .build();

        Mockito.when(repository.findByEntityAndTimestampAndExchange(101, now, "basic"))
                .thenReturn(Uni.createFrom().item(
                        Collections.singletonList(ExchangeAttemptDO.builder()
                                .entityId(101L)
                                .exchangeFrom("basic")
                                .exchangeTo("session")
                                .build())
                ));

        final Collection<ExchangeAttemptBO> actual = service.find(query);
        final Collection<ExchangeAttemptBO> expected = Collections.singletonList(ExchangeAttemptBO.builder()
                .entityId(101L)
                .exchangeFrom("basic")
                .exchangeTo("session")
                .build());

        assertThat(actual).isEqualTo(expected);
    }
}