package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AccountLockBO;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountLocksService {
    CompletableFuture<AccountLockBO> create(AccountLockBO accountLock);

    CompletableFuture<Collection<AccountLockBO>> getActiveLocksByAccountId(long accountId);

    CompletableFuture<Optional<AccountLockBO>> delete(long lockId);
}
