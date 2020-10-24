package com.authguard.service;

import com.authguard.service.model.Entity;
import com.authguard.service.model.IdempotentRecordBO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface IdempotencyService {
    CompletableFuture<IdempotentRecordBO> create(IdempotentRecordBO record);

    CompletableFuture<Optional<IdempotentRecordBO>> findByKeyAndEntityType(String idempotentKey, String entityType);

    <T extends Entity> CompletableFuture<T> performOperation(Supplier<T> operation,
                                                             String idempotentKey,
                                                             String entityType);
}
