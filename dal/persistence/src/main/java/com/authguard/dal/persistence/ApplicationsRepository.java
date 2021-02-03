package com.authguard.dal.persistence;

import com.authguard.dal.model.AppDO;
import com.authguard.dal.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApplicationsRepository extends Repository<AppDO> {
    CompletableFuture<Optional<AppDO>> getByExternalId(String externalId);
    CompletableFuture<List<AppDO>> getAllForAccount(String accountId);
}
