package com.authguard.dal;

import com.authguard.dal.model.AccountLockDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountLocksRepository {
    CompletableFuture<Optional<AccountLockDO>> findByAccountId(String accountId);

    CompletableFuture<AccountLockDO> save(AccountLockDO accountLock);

    CompletableFuture<Optional<AccountLockDO>> deleteByAccountId(String accountId);
}
