package com.authguard.dal;

import com.authguard.dal.model.IdempotentRecordDO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IdempotentRecordsRepository {
    CompletableFuture<IdempotentRecordDO> save(IdempotentRecordDO record);
    CompletableFuture<List<IdempotentRecordDO>> findByKey(String idempotentKey);
    CompletableFuture<Optional<IdempotentRecordDO>> findByKeyAndEntityType(String idempotentKey, String entityType);
}
