package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AccountLockBO;

import java.util.Collection;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface AccountLocksService {
    Uni<AccountLockBO> create(AccountLockBO accountLock);

    Uni<Collection<AccountLockBO>> getActiveLocksByAccountId(long accountId);

    Uni<Optional<AccountLockBO>> delete(long lockId);
}
