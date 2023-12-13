package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AccountLockBO;

import java.util.Collection;
import java.util.Optional;

public interface AccountLocksService {
    AccountLockBO create(AccountLockBO accountLock);

    Collection<AccountLockBO> getActiveLocksByAccountId(long accountId);

    Optional<AccountLockBO> delete(long lockId);
}
