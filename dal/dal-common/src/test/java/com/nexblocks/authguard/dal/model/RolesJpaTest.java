package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RolesJpaTest {
    private EntityManager entityManager;
    private RoleDO first;
    private RoleDO second;
    private RoleDO deleted;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(RoleDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create entities
        first = RoleDO.builder()
                .id(1)
                .name("role-1")
                .domain("main")
                .build();

        second = RoleDO.builder()
                .id(2)
                .name("role-2")
                .domain("main")
                .build();

        deleted = RoleDO.builder()
                .id(3)
                .deleted(true)
                .name("role-3")
                .domain("main")
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.persist(deleted);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final TypedQuery<RoleDO> query = entityManager.createNamedQuery("roles.getById", RoleDO.class)
                .setParameter("id", first.getId());

        final List<RoleDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first);
    }

    @Test
    void getAll() {
        final TypedQuery<RoleDO> query = entityManager.createNamedQuery("roles.getAll", RoleDO.class)
                .setParameter("domain", first.getDomain())
                .setParameter("cursor", 0L);

        final List<RoleDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first, second);
    }

    @Test
    void getByName() {
        final TypedQuery<RoleDO> query = entityManager.createNamedQuery("roles.getByName", RoleDO.class)
                .setParameter("name", first.getName())
                .setParameter("domain", first.getDomain());

        final List<RoleDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first);
    }

    @Test
    void getMultiple() {
        final TypedQuery<RoleDO> query = entityManager.createNamedQuery("roles.getMultiple", RoleDO.class)
                .setParameter("names", Arrays.asList(first.getName(), second.getName()))
                .setParameter("domain", first.getDomain());

        final List<RoleDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first, second);
    }

    @Test
    void getDeletedById() {
        final TypedQuery<RoleDO> query = entityManager.createNamedQuery("roles.getById", RoleDO.class)
                .setParameter("id", deleted.getId());

        final List<RoleDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }
}
