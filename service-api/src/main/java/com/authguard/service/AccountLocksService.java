package com.authguard.service;

import com.authguard.service.model.AccountLockBO;

import java.util.Collection;
import java.util.Optional;

public interface AccountLocksService {
    Collection<AccountLockBO> getActiveLocksByAccountId(String accountId);

    Optional<AccountLockBO> delete(String lockId);
}
