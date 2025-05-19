package com.nexblocks.authguard.dal.cache;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import io.smallrye.mutiny.Uni;

import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface AccountTokensRepository {
    Uni<AccountTokenDO> save(AccountTokenDO tokenDO);
    Uni<Optional<AccountTokenDO>> getByToken(String token);
    Uni<Optional<AccountTokenDO>> deleteToken(String token);
}
