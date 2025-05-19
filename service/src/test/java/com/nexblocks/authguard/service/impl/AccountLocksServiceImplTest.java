package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.cache.AccountLocksRepository;
import com.nexblocks.authguard.dal.model.AccountLockDO;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountLockBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLocksServiceImplTest {

    private AccountLocksRepository repository;
    private AccountLocksService service;

    @BeforeEach
    void setup() {
        repository = Mockito.mock(AccountLocksRepository.class);
        service = new AccountLocksServiceImpl(repository, new ServiceMapperImpl());
    }

    @Test
    void getActiveLocksByAccountId() {
        Instant now = Instant.now();

        Mockito.when(repository.findByAccountId(101))
                .thenReturn(Uni.createFrom().item(
                        Arrays.asList(
                                AccountLockDO.builder()
                                        .accountId(101)
                                        .expiresAt(now.plus(Duration.ofMinutes(5)))
                                        .build(),
                                AccountLockDO.builder()
                                        .accountId(101)
                                        .expiresAt(now.minus(Duration.ofMinutes(1)))
                                        .build()
                        )
                ));

        Collection<AccountLockBO> actual = service.getActiveLocksByAccountId(101).join();
        Collection<AccountLockBO> expected = Collections.singletonList(AccountLockBO.builder()
                .accountId(101L)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .build());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void delete() {
        Instant now = Instant.now();

        Mockito.when(repository.delete(1))
                .thenReturn(Uni.createFrom().item(
                        Optional.of(AccountLockDO.builder()
                                .accountId(101)
                                .expiresAt(now.plus(Duration.ofMinutes(5)))
                                .build())
                ));

        Optional<AccountLockBO> actual = service.delete(1).join();
        AccountLockBO expected = AccountLockBO.builder()
                .accountId(101L)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .build();

        assertThat(actual).contains(expected);
    }
}