package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.AccountTokenDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountTokensRepository {
    CompletableFuture<AccountTokenDO> save(AccountTokenDO tokenDO);
    CompletableFuture<Optional<AccountTokenDO>> getByToken(String token);
}
