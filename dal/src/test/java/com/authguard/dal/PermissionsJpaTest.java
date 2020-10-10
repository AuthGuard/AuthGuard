package com.authguard.dal;

import com.authguard.dal.model.PermissionDO;
import com.authguard.dal.model.RoleDO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PermissionsJpaTest {
    private EntityManager entityManager;
    private PermissionDO first;
    private PermissionDO second;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(PermissionDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create entities
        first = PermissionDO.builder()
                .id("first-permission")
                .group("test")
                .name("read")
                .build();

        second = PermissionDO.builder()
                .id("second-permission")
                .group("test")
                .name("write")
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(first);
        entityManager.persist(second);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final PermissionDO retrieved = entityManager.find(PermissionDO.class, first.getId());

        assertThat(retrieved).isEqualTo(first);
    }

    @Test
    void getAll() {
        final TypedQuery<PermissionDO> query = entityManager.createNamedQuery("permissions.getAll", PermissionDO.class);

        final List<PermissionDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(first, second);
    }

    @Test
    void getByGroupAndName() {
        final TypedQuery<PermissionDO> query = entityManager.createNamedQuery("permissions.getByGroupAndName", PermissionDO.class)
                .setParameter("group", first.getGroup())
                .setParameter("name", first.getName());

        final List<PermissionDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(first);
    }

    @Test
    void getByGroup() {
        final TypedQuery<PermissionDO> query = entityManager.createNamedQuery("permissions.getByGroup", PermissionDO.class)
                .setParameter("group", first.getGroup());

        final List<PermissionDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(first, second);
    }
}
