package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.AccountDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AccountsRepository extends Repository<AccountDO> {
    CompletableFuture<Optional<AccountDO>> getByExternalId(String externalId);
    CompletableFuture<Optional<AccountDO>> getByEmail(String email, String domain);
    CompletableFuture<List<AccountDO>> getByRole(String role, String domain);
    CompletableFuture<Optional<AccountDO>> findByIdentifier(String identifier, final String domain);
}
