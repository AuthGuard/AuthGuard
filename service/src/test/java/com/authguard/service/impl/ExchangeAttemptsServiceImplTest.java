package com.authguard.service.impl;

import com.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.authguard.dal.model.ExchangeAttemptDO;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.ExchangeAttemptBO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeAttemptsServiceImplTest {

    @Test
    void get() {
        final ExchangeAttemptsRepository repository = Mockito.mock(ExchangeAttemptsRepository.class);
        final ExchangeAttemptsServiceImpl service = new ExchangeAttemptsServiceImpl(repository, new ServiceMapperImpl());

        Mockito.when(repository.findByEntity("account"))
                .thenReturn(CompletableFuture.completedFuture(
                        Collections.singletonList(ExchangeAttemptDO.builder()
                                .entityId("account")
                                .exchangeFrom("basic")
                                .exchangeTo("session")
                                .build())
                ));

        final Collection<ExchangeAttemptBO> actual = service.get("account");
        final Collection<ExchangeAttemptBO> expected = Collections.singletonList(ExchangeAttemptBO.builder()
                .entityId("account")
                .exchangeFrom("basic")
                .exchangeTo("session")
                .build());

        assertThat(actual).isEqualTo(expected);
    }
}