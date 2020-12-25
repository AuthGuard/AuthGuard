package com.authguard.service.impl;

import com.authguard.dal.persistence.IdempotentRecordsRepository;
import com.authguard.service.IdempotencyService;
import com.authguard.service.exceptions.IdempotencyException;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.Entity;
import com.authguard.service.model.IdempotentRecordBO;
import com.google.inject.Inject;

import java.util.Optional;
import java.util.UUID;
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
        return repository.save(serviceMapper.toDO(record))
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
                            .id(UUID.randomUUID().toString())
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
