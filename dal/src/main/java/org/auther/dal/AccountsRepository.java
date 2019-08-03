package org.auther.dal;

import org.auther.dal.model.AccountDO;

import java.util.Optional;

public interface AccountsRepository {
    AccountDO save(AccountDO account);
    Optional<AccountDO> getById(String accountId);
    Optional<AccountDO> findByUsername(String username);
}
