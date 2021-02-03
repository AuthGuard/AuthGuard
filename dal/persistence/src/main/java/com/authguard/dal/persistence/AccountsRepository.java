package com.authguard.dal.persistence;

import com.authguard.dal.model.AccountDO;
import com.authguard.dal.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountsRepository extends Repository<AccountDO> {
    CompletableFuture<Optional<AccountDO>> getByExternalId(String externalId);
    CompletableFuture<List<AccountDO>> getByRole(String role);
}
