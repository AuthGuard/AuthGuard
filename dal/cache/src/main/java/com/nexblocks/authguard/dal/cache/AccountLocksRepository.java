package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.AccountLockDO;
import io.smallrye.mutiny.Uni;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountLocksRepository {
    CompletableFuture<Collection<AccountLockDO>> findByAccountId(long accountId);

    Uni<AccountLockDO> save(AccountLockDO accountLock);

    Uni<Optional<AccountLockDO>> delete(long id);
}
