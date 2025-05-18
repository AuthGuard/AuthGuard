package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IdempotentRecordJpaTest {
    private EntityManager entityManager;
    private IdempotentRecordDO first;
    private IdempotentRecordDO second;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(IdempotentRecordDO.class)
                .prepare();
        entityManager = h2.getEntityManager();

        // create record
        first = IdempotentRecordDO.builder()
                .id(1)
                .idempotentKey("idempotent-key")
                .entityType("FirstEntity")
                .entityId(101)
                .build();

        second = IdempotentRecordDO.builder()
                .id(2)
                .idempotentKey("idempotent-key")
                .entityType("SecondEntity")
                .entityId(102)
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(first);
        entityManager.persist(second);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final IdempotentRecordDO retrieved = entityManager.find(IdempotentRecordDO.class, first.getId());

        assertThat(retrieved).isEqualTo(first);
    }

    @Test
    void getByKey() {
        final TypedQuery<IdempotentRecordDO> query = entityManager.createNamedQuery("idempotent_records.getByKey", IdempotentRecordDO.class)
                .setParameter("key", first.getIdempotentKey());

        final List<IdempotentRecordDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first, second);
    }

    @Test
    void getByKeyAndEntityType() {
        final TypedQuery<IdempotentRecordDO> query = entityManager.createNamedQuery("idempotent_records.getByKeyAndEntity", IdempotentRecordDO.class)
                .setParameter("key", first.getIdempotentKey())
                .setParameter("entityType", first.getEntityType());

        final List<IdempotentRecordDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first);
    }
}
