package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PermissionsJpaTest {
    private EntityManager entityManager;
    private PermissionDO first;
    private PermissionDO second;
    private PermissionDO deleted;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(PermissionDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create entities
        first = PermissionDO.builder()
                .id(1)
                .group("test")
                .name("read")
                .domain("main")
                .build();

        second = PermissionDO.builder()
                .id(2)
                .group("test")
                .name("write")
                .domain("main")
                .build();

        deleted = PermissionDO.builder()
                .id(3)
                .deleted(true)
                .group("test")
                .name("delete")
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
        final TypedQuery<PermissionDO> query = entityManager.createNamedQuery("permissions.getById", PermissionDO.class)
                .setParameter("id", first.getId());

        final List<PermissionDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first);
    }

    @Test
    void getAll() {
        final TypedQuery<PermissionDO> query = entityManager.createNamedQuery("permissions.getAll", PermissionDO.class)
                .setParameter("domain", first.getDomain());

        final List<PermissionDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first, second);
    }

    @Test
    void getByGroupAndName() {
        final TypedQuery<PermissionDO> query = entityManager.createNamedQuery("permissions.getByGroupAndName", PermissionDO.class)
                .setParameter("group", first.getGroup())
                .setParameter("name", first.getName())
                .setParameter("domain", first.getDomain());

        final List<PermissionDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first);
    }

    @Test
    void getByGroup() {
        final TypedQuery<PermissionDO> query = entityManager.createNamedQuery("permissions.getByGroup", PermissionDO.class)
                .setParameter("group", first.getGroup())
                .setParameter("domain", first.getDomain());

        final List<PermissionDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(first, second);
    }

    @Test
    void getDeletedById() {
        final TypedQuery<PermissionDO> query = entityManager.createNamedQuery("permissions.getById", PermissionDO.class)
                .setParameter("id", deleted.getId());

        final List<PermissionDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }
}
