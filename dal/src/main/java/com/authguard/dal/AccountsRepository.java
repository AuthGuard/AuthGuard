package com.authguard.dal;

import com.authguard.dal.common.Repository;
import com.authguard.dal.model.AccountDO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountsRepository extends Repository<AccountDO> {;
    CompletableFuture<Optional<AccountDO>> getByExternalId(String externalId);
    CompletableFuture<List<AccountDO>> getByRole(String role);
}
