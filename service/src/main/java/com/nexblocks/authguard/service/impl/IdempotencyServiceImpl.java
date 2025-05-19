package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.persistence.IdempotentRecordsRepository;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.exceptions.IdempotencyException;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.Entity;
import com.nexblocks.authguard.service.model.IdempotentRecordBO;
import com.nexblocks.authguard.service.util.ID;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class IdempotencyServiceImpl implements IdempotencyService {
    private final IdempotentRecordsRepository repository;
    private final ServiceMapper serviceMapper;

    @Inject
    public IdempotencyServiceImpl(final IdempotentRecordsRepository repository,
                                  final ServiceMapper serviceMapper) {
        this.repository = repository;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public CompletableFuture<IdempotentRecordBO> create(final IdempotentRecordBO record) {
        return repository.save(serviceMapper.toDO(record)).subscribe().asCompletionStage()
                .thenApply(serviceMapper::toBO);
    }

    @Override
    public CompletableFuture<Optional<IdempotentRecordBO>> findByKeyAndEntityType(final String idempotentKey,
                                                                                  final String entityType) {
        return repository.findByKeyAndEntityType(idempotentKey, entityType)
                .thenApply(recordOptional -> recordOptional.map(serviceMapper::toBO));
    }

    @Override
    public <T extends Entity> CompletableFuture<T> performOperation(final Supplier<T> operation,
                                                                    final String idempotentKey,
                                                                    final String entityType) {
        return findByKeyAndEntityType(idempotentKey, entityType)
                .thenApplyAsync(record -> {
                    if (record.isPresent()) {
                        throw new IdempotencyException(record.get());
                    }

                    return operation.get();
                })
                .thenApply(result -> {
                    final IdempotentRecordBO record = IdempotentRecordBO.builder()
                            .id(ID.generate())
                            .entityId(result.getId())
                            .entityType(result.getEntityType())
                            .idempotentKey(idempotentKey)
                            .build();

                    // we don't have to wait for this to finish
                    CompletableFuture.runAsync(() -> create(record));

                    return result;
                });
    }

    @Override
    public <T extends Entity> CompletableFuture<T> performOperationAsync(Supplier<CompletableFuture<T>> operation, String idempotentKey, String entityType) {
        return findByKeyAndEntityType(idempotentKey, entityType)
                .thenCompose(record -> {
                    if (record.isPresent()) {
                        throw new IdempotencyException(record.get());
                    }

                    return operation.get();
                })
                .thenApply(result -> {
                    final IdempotentRecordBO record = IdempotentRecordBO.builder()
                            .id(ID.generate())
                            .entityId(result.getId())
                            .entityType(result.getEntityType())
                            .idempotentKey(idempotentKey)
                            .build();

                    // we don't have to wait for this to finish
                    CompletableFuture.runAsync(() -> create(record));

                    return result;
                });
    }
}
