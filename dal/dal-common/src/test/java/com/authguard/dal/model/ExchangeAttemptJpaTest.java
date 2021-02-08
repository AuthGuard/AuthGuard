package com.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.OffsetDateTime;
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
                .id("first-attempt")
                .entityId("account")
                .exchangeFrom("basic")
                .exchangeTo("unknown")
                .createdAt(OffsetDateTime.now().minusHours(3))
                .build();

        secondAttempt = ExchangeAttemptDO.builder()
                .id("second-attempt")
                .entityId("account")
                .exchangeFrom("jwt")
                .exchangeTo("unknown")
                .createdAt(OffsetDateTime.now().minusHours(1))
                .build();

        thirdAttempt = ExchangeAttemptDO.builder()
                .id("third-attempt")
                .entityId("application")
                .exchangeFrom("id")
                .exchangeTo("api")
                .createdAt(OffsetDateTime.now().minusHours(1))
                .build();

        entityManager.persist(firstAttempt);
        entityManager.persist(secondAttempt);
        entityManager.persist(thirdAttempt);

        entityManager.getTransaction().commit();
    }

    @Test
    void getByEntityId() {
        final TypedQuery<ExchangeAttemptDO> query = entityManager.createNamedQuery("exchange_attempts.getByEntityId",
                ExchangeAttemptDO.class).setParameter("entityId", "account");

        final List<ExchangeAttemptDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(firstAttempt, secondAttempt);
    }

    @Test
    void getByEntityIdFromTimestamp() {
        final TypedQuery<ExchangeAttemptDO> query = entityManager.createNamedQuery(
                "exchange_attempts.getByEntityIdFromTimestamp", ExchangeAttemptDO.class)
                .setParameter("entityId", "account")
                .setParameter("timestamp", OffsetDateTime.now().minusHours(2));

        final List<ExchangeAttemptDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(secondAttempt);
    }

    @Test
    void getByEntityIdAndExchangeFromTimestamp() {
        final TypedQuery<ExchangeAttemptDO> query = entityManager.createNamedQuery(
                "exchange_attempts.getByEntityIdAndExchangeFromTimestamp", ExchangeAttemptDO.class)
                .setParameter("entityId", "account")
                .setParameter("timestamp", OffsetDateTime.now().minusHours(4))
                .setParameter("exchangeFrom", "basic");

        final List<ExchangeAttemptDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(firstAttempt);
    }
}
