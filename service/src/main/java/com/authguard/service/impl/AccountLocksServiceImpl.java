package com.authguard.service.impl;

import com.authguard.dal.AccountLocksRepository;
import com.authguard.service.AccountLocksService;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.AccountLockBO;
import com.google.inject.Inject;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountLocksServiceImpl implements AccountLocksService {
    private final AccountLocksRepository accountLocksRepository;
    private final ServiceMapper serviceMapper;

    @Inject
    public AccountLocksServiceImpl(final AccountLocksRepository accountLocksRepository,
                                   final ServiceMapper serviceMapper) {
        this.accountLocksRepository = accountLocksRepository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public Collection<AccountLockBO> getActiveLocksByAccountId(final String accountId) {
        final OffsetDateTime now = OffsetDateTime.now();

        return accountLocksRepository.findByAccountId(accountId)
                .thenApply(locks -> locks.stream()
                        .filter(lock -> lock.getExpiresAt().isAfter(now))
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList())
                ).join();
    }

    @Override
    public Optional<AccountLockBO> delete(final String lockId) {
        return accountLocksRepository.delete(lockId)
                .thenApply(lock -> lock.map(serviceMapper::toBO))
                .join();
    }
}
