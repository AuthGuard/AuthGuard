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
import io.smallrye.mutiny.Uni;
import java.util.function.Function;
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
    public Uni<AccountLockBO> create(final AccountLockBO accountLock) {
        final AccountLockDO accountLockDO = serviceMapper.toDO(accountLock);

        LOG.info("Locking an account. accountId={}, expiresAt={}", accountLock.getAccountId(), accountLock.getExpiresAt());

        return accountLocksRepository.save(accountLockDO)
                .map(serviceMapper::toBO);
    }

    @Override
    public Uni<Collection<AccountLockBO>> getActiveLocksByAccountId(final long accountId) {
        final Instant now = Instant.now();

        return accountLocksRepository.findByAccountId(accountId)
                .map(locks -> locks.stream()
                        .filter(lock -> lock.getExpiresAt().isAfter(now))
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList())
                )
                .map(Function.identity());
    }

    @Override
    public Uni<Optional<AccountLockBO>> delete(final long lockId) {
        return accountLocksRepository.delete(lockId)
                .map(lock -> lock.map(serviceMapper::toBO));
    }
}
