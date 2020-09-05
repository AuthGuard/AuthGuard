package com.authguard.dal;

import com.authguard.dal.model.ApiKeyDO;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApiKeysRepository {
    CompletableFuture<ApiKeyDO> save(ApiKeyDO apiKey);
    CompletableFuture<Optional<ApiKeyDO>> getById(String id);
    CompletableFuture<Collection<ApiKeyDO>> getByAppId(String id);
    CompletableFuture<Optional<ApiKeyDO>> update(ApiKeyDO apiKey);
    CompletableFuture<Optional<ApiKeyDO>> delete(String id);
}
