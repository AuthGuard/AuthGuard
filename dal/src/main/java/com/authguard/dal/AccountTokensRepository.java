package com.authguard.dal;

import com.authguard.dal.model.AccountTokenDO;

import java.util.Optional;

public interface AccountTokensRepository {
    AccountTokenDO save(AccountTokenDO tokenDO);
    Optional<AccountTokenDO> getByToken(String token);
}
