package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import io.smallrye.mutiny.Uni;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountTokensRepository {
    Uni<AccountTokenDO> save(AccountTokenDO tokenDO);
    CompletableFuture<Optional<AccountTokenDO>> getByToken(String token);
    CompletableFuture<Optional<AccountTokenDO>> deleteToken(String token);
}
