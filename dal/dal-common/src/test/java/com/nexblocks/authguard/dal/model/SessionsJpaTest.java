package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SessionsJpaTest {
    private EntityManager entityManager;
    private SessionDO first;
    private SessionDO second;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(SessionDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create entities
        first = SessionDO.builder()
                .id(1)
                .sessionToken("token-1")
                .build();

        second = SessionDO.builder()
                .id(2)
                .sessionToken("token-2")
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(first);
        entityManager.persist(second);

        entityManager.getTransaction().commit();
    }

    @Test
    void getByToken() {
        final TypedQuery<SessionDO> query = entityManager.createNamedQuery("sessions.getByToken", SessionDO.class)
                .setParameter("token", first.getSessionToken());

        final List<SessionDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsOnly(first);
    }

    @Test
    void insertDuplicateToken() {
        entityManager.getTransaction().begin();

        entityManager.persist(SessionDO.builder()
                .id(3)
                .sessionToken(first.getSessionToken())
                .build());

        assertThatThrownBy(() -> entityManager.getTransaction().commit())
                .isInstanceOf(ConstraintViolationException.class);
    }
}
