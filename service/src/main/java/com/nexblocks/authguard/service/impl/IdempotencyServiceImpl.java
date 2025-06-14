package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.dal.persistence.IdempotentRecordsRepository;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.exceptions.IdempotencyException;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.Entity;
import com.nexblocks.authguard.service.model.IdempotentRecordBO;
import com.nexblocks.authguard.service.util.ID;
import io.smallrye.mutiny.Uni;

import java.util.Optional;
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
    public Uni<IdempotentRecordBO> create(final IdempotentRecordBO record) {
        return repository.save(serviceMapper.toDO(record))
                .map(serviceMapper::toBO);
    }

    @Override
    public Uni<Optional<IdempotentRecordBO>> findByKeyAndEntityType(final String idempotentKey,
                                                                    final String entityType) {
        return repository.findByKeyAndEntityType(idempotentKey, entityType)
                .map(recordOptional -> recordOptional.map(serviceMapper::toBO));
    }

    @Override
    public <T extends Entity> Uni<T> performOperation(final Supplier<T> operation,
                                                                    final String idempotentKey,
                                                                    final String entityType) {
        return findByKeyAndEntityType(idempotentKey, entityType)
                .map(record -> {
                    if (record.isPresent()) {
                        throw new IdempotencyException(record.get());
                    }

                    return operation.get();
                })
                .map(result -> {
                    final IdempotentRecordBO record = IdempotentRecordBO.builder()
                            .id(ID.generate())
                            .entityId(result.getId())
                            .entityType(result.getEntityType())
                            .idempotentKey(idempotentKey)
                            .build();

                    // we don't have to wait for this to finish
                    create(record).subscribe()
                            .with(ignored -> {});

                    return result;
                });
    }

    @Override
    public <T extends Entity> Uni<T> performOperationAsync(Supplier<Uni<T>> operation, String idempotentKey, String entityType) {
        return findByKeyAndEntityType(idempotentKey, entityType)
                .flatMap(record -> {
                    if (record.isPresent()) {
                        throw new IdempotencyException(record.get());
                    }

                    return operation.get();
                })
                .map(result -> {
                    final IdempotentRecordBO record = IdempotentRecordBO.builder()
                            .id(ID.generate())
                            .entityId(result.getId())
                            .entityType(result.getEntityType())
                            .idempotentKey(idempotentKey)
                            .build();

                    // we don't have to wait for this to finish
                    create(record).subscribe()
                            .with(ignored -> {});

                    return result;
                });
    }
}
