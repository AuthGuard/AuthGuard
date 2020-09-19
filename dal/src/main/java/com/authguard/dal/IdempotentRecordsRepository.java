package com.authguard.dal;

import com.authguard.dal.model.IdempotentRecordDO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IdempotentRecordsRepository {
    CompletableFuture<IdempotentRecordDO> save(IdempotentRecordDO record);
    CompletableFuture<Optional<IdempotentRecordDO>> findByKey(String idempotentKey);
}
