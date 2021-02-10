package com.nexblocks.authguard.dal.persistence;

import com.nexblocks.authguard.dal.model.ApiKeyDO;
import com.nexblocks.authguard.dal.repository.ImmutableRecordRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ApiKeysRepository extends ImmutableRecordRepository<ApiKeyDO> {
    CompletableFuture<Collection<ApiKeyDO>> getByAppId(String id);
    CompletableFuture<Optional<ApiKeyDO>> getByKey(String key);
}
