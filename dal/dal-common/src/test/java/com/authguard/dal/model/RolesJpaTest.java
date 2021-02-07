package com.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
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
                .id("first-role")
                .name("role-1")
                .build();

        second = RoleDO.builder()
                .id("second-role")
                .name("role-2")
                .build();

        deleted = RoleDO.builder()
                .id("deleted-role")
                .deleted(true)
                .name("role-3")
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
        final TypedQuery<RoleDO> query = entityManager.createNamedQuery("roles.getAll", RoleDO.class);

        final List<RoleDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first, second);
    }

    @Test
    void getByName() {
        final TypedQuery<RoleDO> query = entityManager.createNamedQuery("roles.getByName", RoleDO.class)
                .setParameter("name", first.getName());

        final List<RoleDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first);
    }

    @Test
    void getMultiple() {
        final TypedQuery<RoleDO> query = entityManager.createNamedQuery("roles.getMultiple", RoleDO.class)
                .setParameter("names", Arrays.asList(first.getName(), second.getName()));

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
