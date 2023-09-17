package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.ClientDO;
import com.nexblocks.authguard.dal.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ClientsRepository extends Repository<ClientDO> {
    CompletableFuture<Optional<ClientDO>> getByExternalId(String externalId);
    CompletableFuture<List<ClientDO>> getAllForAccount(String accountId);
    CompletableFuture<List<ClientDO>> getByType(String externalId);
    CompletableFuture<List<ClientDO>> getByDomain(String externalId);
}
