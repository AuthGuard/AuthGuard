package com.authguard.service.impl;

import com.authguard.dal.persistence.IdempotentRecordsRepository;
import com.authguard.dal.model.IdempotentRecordDO;
import com.authguard.service.IdempotencyService;
import com.authguard.service.exceptions.IdempotencyException;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyServiceImplTest {
    private static final String ENTITY_TYPE = "TestEntity";
    private IdempotentRecordsRepository repository;
    private IdempotencyService service;

    static class TestEntity implements Entity {
        private final String id;

        TestEntity(final String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getEntityType() {
            return ENTITY_TYPE;
        }
    }

    @BeforeEach
    void setup() {
        repository = Mockito.mock(IdempotentRecordsRepository.class);

        service = new IdempotencyServiceImpl(repository, new ServiceMapperImpl());
    }

    @Test
    void perform() throws InterruptedException {
        final String idempotentKey = UUID.randomUUID().toString();
        final TestEntity entity = new TestEntity("entity-id");
        final Supplier<TestEntity> operation = () -> entity;

        Mockito.when(repository.findByKeyAndEntityType(idempotentKey, ENTITY_TYPE))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Mockito.when(repository.save(Mockito.any())).thenAnswer(Mockito.RETURNS_DEEP_STUBS);

        final TestEntity result = service.performOperation(operation, idempotentKey, ENTITY_TYPE).join();
        final IdempotentRecordDO expectedRecord = IdempotentRecordDO.builder()
                .idempotentKey(idempotentKey)
                .entityId(entity.getId())
                .entityType(entity.getEntityType())
                .build();

        assertThat(result).isEqualTo(entity);

        Thread.sleep(1000L); // give it some time to make sure that save() was called

        Mockito.verify(repository).save(expectedRecord);
    }

    @Test
    void performExistingKey() {
        final String idempotentKey = UUID.randomUUID().toString();
        final TestEntity entity = new TestEntity("entity-id");

        final IdempotentRecordDO idempotentRecord = IdempotentRecordDO.builder()
                .idempotentKey(idempotentKey)
                .entityId(entity.getId())
                .entityType(entity.getEntityType())
                .build();

        final Supplier<TestEntity> operation = () -> entity;

        Mockito.when(repository.findByKeyAndEntityType(idempotentKey, ENTITY_TYPE))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(idempotentRecord)));

        Mockito.when(repository.save(Mockito.any())).thenAnswer(Mockito.RETURNS_DEEP_STUBS);

        assertThatThrownBy(() -> service.performOperation(operation, idempotentKey, ENTITY_TYPE).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IdempotencyException.class);
    }
}