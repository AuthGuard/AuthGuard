package org.auther.dal;

import org.auther.dal.model.AccountTokenDO;

import java.util.Optional;

public interface AccountTokensRepository {
    AccountTokenDO save(AccountTokenDO tokenDO);
    Optional<AccountTokenDO> getByToken(String token);
}
