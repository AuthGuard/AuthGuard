package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.Entity;
import com.nexblocks.authguard.service.model.IdempotentRecordBO;

import java.util.Optional;
import io.smallrye.mutiny.Uni;
import java.util.function.Supplier;

public interface IdempotencyService {
    Uni<IdempotentRecordBO> create(IdempotentRecordBO record);

    Uni<Optional<IdempotentRecordBO>> findByKeyAndEntityType(String idempotentKey, String entityType);

    <T extends Entity> Uni<T> performOperation(Supplier<T> operation,
                                                             String idempotentKey,
                                                             String entityType);

    <T extends Entity> Uni<T> performOperationAsync(Supplier<Uni<T>> operation,
                                                                  String idempotentKey,
                                                                  String entityType);
}
