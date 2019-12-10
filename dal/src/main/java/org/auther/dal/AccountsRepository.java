package org.auther.dal;

import org.auther.dal.model.AccountDO;

import java.util.List;
import java.util.Optional;

public interface AccountsRepository {
    AccountDO save(AccountDO account);
    Optional<AccountDO> getById(String accountId);
    Optional<AccountDO> update(AccountDO account);
    List<AccountDO> getAdmins();
}
