package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.AccountLockDO;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountLocksRepository {
    CompletableFuture<Collection<AccountLockDO>> findByAccountId(String accountId);

    CompletableFuture<AccountLockDO> save(AccountLockDO accountLock);

    CompletableFuture<Optional<AccountLockDO>> delete(String id);
}
