package com.authguard.dal;

import com.authguard.dal.model.AccountDO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountsRepository {
    CompletableFuture<AccountDO> save(AccountDO account);
    CompletableFuture<Optional<AccountDO>> getById(String accountId);
    CompletableFuture<Optional<AccountDO>> getByExternalId(String externalId);
    CompletableFuture<Optional<AccountDO>> update(AccountDO account);
    CompletableFuture<Optional<AccountDO>> delete(String accountId);
    CompletableFuture<List<AccountDO>> getByRole(String role);
}
