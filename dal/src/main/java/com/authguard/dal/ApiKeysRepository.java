package com.authguard.dal;

import com.authguard.dal.common.ImmutableRecordRepository;
import com.authguard.dal.model.ApiKeyDO;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface ApiKeysRepository extends ImmutableRecordRepository<ApiKeyDO> {
    CompletableFuture<Collection<ApiKeyDO>> getByAppId(String id);
}
