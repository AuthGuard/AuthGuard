package com.authguard.dal;

import com.authguard.dal.common.Repository;
import com.authguard.dal.model.AppDO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApplicationsRepository extends Repository<AppDO> {
    CompletableFuture<Optional<AppDO>> getByExternalId(String externalId);
    CompletableFuture<List<AppDO>> getAllForAccount(String accountId);
}
