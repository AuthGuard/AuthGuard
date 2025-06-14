package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.persistence.IdempotentRecordsRepository;
import com.nexblocks.authguard.dal.model.IdempotentRecordDO;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.exceptions.IdempotencyException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.Entity;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyServiceImplTest {
    private static final String ENTITY_TYPE = "TestEntity";
    private IdempotentRecordsRepository repository;
    private IdempotencyService service;

    static class TestEntity implements Entity {
        private final long id;
        private final Instant now;

        TestEntity(final long id) {
            this.id = id;
            this.now = Instant.now();
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getDomain() {
            return "main";
        }

        @Override
        public Instant getCreatedAt() {
            return now;
        }

        @Override
        public Instant getLastModified() {
            return now;
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
        final TestEntity entity = new TestEntity(1);
        final Supplier<TestEntity> operation = () -> entity;

        Mockito.when(repository.findByKeyAndEntityType(idempotentKey, ENTITY_TYPE))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        Mockito.when(repository.save(Mockito.any())).thenAnswer(Mockito.RETURNS_DEEP_STUBS);

        final TestEntity result = service.performOperation(operation, idempotentKey, ENTITY_TYPE).subscribeAsCompletionStage().join();
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
        final TestEntity entity = new TestEntity(2);

        final IdempotentRecordDO idempotentRecord = IdempotentRecordDO.builder()
                .idempotentKey(idempotentKey)
                .entityId(entity.getId())
                .entityType(entity.getEntityType())
                .build();

        final Supplier<TestEntity> operation = () -> entity;

        Mockito.when(repository.findByKeyAndEntityType(idempotentKey, ENTITY_TYPE))
                .thenReturn(Uni.createFrom().item(Optional.of(idempotentRecord)));

        Mockito.when(repository.save(Mockito.any())).thenAnswer(Mockito.RETURNS_DEEP_STUBS);

        assertThatThrownBy(() -> service.performOperation(operation, idempotentKey, ENTITY_TYPE).subscribeAsCompletionStage().join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IdempotencyException.class);
    }
}