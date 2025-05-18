package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExchangeAttemptJpaTest {
    private EntityManager entityManager;
    private ExchangeAttemptDO firstAttempt;
    private ExchangeAttemptDO secondAttempt;
    private ExchangeAttemptDO thirdAttempt;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2().withMappedClass(ExchangeAttemptDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create the test attempts
        entityManager.getTransaction().begin();

        firstAttempt = ExchangeAttemptDO.builder()
                .id(1)
                .entityId(101)
                .exchangeFrom("basic")
                .exchangeTo("unknown")
                .createdAt(Instant.now().minus(Duration.ofHours(3)))
                .build();

        secondAttempt = ExchangeAttemptDO.builder()
                .id(2)
                .entityId(101)
                .exchangeFrom("jwt")
                .exchangeTo("unknown")
                .createdAt(Instant.now().minus(Duration.ofHours(1)))
                .build();

        thirdAttempt = ExchangeAttemptDO.builder()
                .id(3)
                .entityId(201)
                .exchangeFrom("id")
                .exchangeTo("api")
                .createdAt(Instant.now().minus(Duration.ofHours(1)))
                .build();

        entityManager.persist(firstAttempt);
        entityManager.persist(secondAttempt);
        entityManager.persist(thirdAttempt);

        entityManager.getTransaction().commit();
    }

    @Test
    void getByEntityId() {
        final TypedQuery<ExchangeAttemptDO> query = entityManager.createNamedQuery("exchange_attempts.getByEntityId",
                ExchangeAttemptDO.class).setParameter("entityId", firstAttempt.getEntityId());

        final List<ExchangeAttemptDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(firstAttempt, secondAttempt);
    }

    @Test
    void getByEntityIdFromTimestamp() {
        final TypedQuery<ExchangeAttemptDO> query = entityManager.createNamedQuery(
                "exchange_attempts.getByEntityIdFromTimestamp", ExchangeAttemptDO.class)
                .setParameter("entityId", firstAttempt.getEntityId())
                .setParameter("timestamp", Instant.now().minus(Duration.ofHours(2)));

        final List<ExchangeAttemptDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(secondAttempt);
    }

    @Test
    void getByEntityIdAndExchangeFromTimestamp() {
        final TypedQuery<ExchangeAttemptDO> query = entityManager.createNamedQuery(
                "exchange_attempts.getByEntityIdAndExchangeFromTimestamp", ExchangeAttemptDO.class)
                .setParameter("entityId", firstAttempt.getEntityId())
                .setParameter("timestamp", Instant.now().minus(Duration.ofHours(4)))
                .setParameter("exchangeFrom", "basic");

        final List<ExchangeAttemptDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(firstAttempt);
    }
}
