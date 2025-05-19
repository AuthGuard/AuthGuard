package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.cache.AccountLocksRepository;
import com.nexblocks.authguard.dal.model.AccountLockDO;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.AccountLockBO;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AccountLocksServiceImpl implements AccountLocksService {
    private static final Logger LOG = LoggerFactory.getLogger(AccountLocksServiceImpl.class);

    private final AccountLocksRepository accountLocksRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public AccountLocksServiceImpl(final AccountLocksRepository accountLocksRepository,
                                   final ServiceMapper serviceMapper) {
        this.accountLocksRepository = accountLocksRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public CompletableFuture<AccountLockBO> create(final AccountLockBO accountLock) {
        final AccountLockDO accountLockDO = serviceMapper.toDO(accountLock);

        LOG.info("Locking an account. accountId={}, expiresAt={}", accountLock.getAccountId(), accountLock.getExpiresAt());

        return accountLocksRepository.save(accountLockDO)
                .subscribeAsCompletionStage()
                .thenApply(serviceMapper::toBO);
    }

    @Override
    public CompletableFuture<Collection<AccountLockBO>> getActiveLocksByAccountId(final long accountId) {
        final Instant now = Instant.now();

        return accountLocksRepository.findByAccountId(accountId)
                .thenApply(locks -> locks.stream()
                        .filter(lock -> lock.getExpiresAt().isAfter(now))
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList())
                );
    }

    @Override
    public CompletableFuture<Optional<AccountLockBO>> delete(final long lockId) {
        return accountLocksRepository.delete(lockId)
                .subscribeAsCompletionStage()
                .thenApply(lock -> lock.map(serviceMapper::toBO));
    }
}
