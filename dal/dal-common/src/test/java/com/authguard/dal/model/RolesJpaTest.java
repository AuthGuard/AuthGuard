package com.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RolesJpaTest {
    private EntityManager entityManager;
    private RoleDO first;
    private RoleDO second;

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

        entityManager.getTransaction().begin();

        entityManager.persist(first);
        entityManager.persist(second);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final RoleDO retrieved = entityManager.find(RoleDO.class, first.getId());

        assertThat(retrieved).isEqualTo(first);
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
}
