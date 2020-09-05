package com.authguard.dal;

import com.authguard.dal.model.AppDO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApplicationsRepository {
    CompletableFuture<AppDO> save(AppDO app);
    CompletableFuture<Optional<AppDO>> getById(String appId);
    CompletableFuture<Optional<AppDO>> getByExternalId(String externalId);
    CompletableFuture<Optional<AppDO>> update(AppDO app);
    CompletableFuture<Optional<AppDO>> delete(String appId);
    CompletableFuture<List<AppDO>> getAllForAccount(String accountId);
}
