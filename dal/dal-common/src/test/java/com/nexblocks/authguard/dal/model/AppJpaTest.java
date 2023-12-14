package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AppJpaTest {
    private EntityManager entityManager;
    private AppDO createdApp;
    private AppDO deletedApp;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(AppDO.class)
                .withMappedClass(PermissionDO.class)
                .withMappedClass(RoleDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create test app
        createdApp = AppDO.builder()
                .id(1)
                .parentAccountId(101L)
                .name("Test Application")
                .externalId("external-id")
                .build();

        deletedApp = AppDO.builder()
                .id(2)
                .deleted(true)
                .parentAccountId(101L)
                .name("Test Application")
                .externalId("external-id")
                .build();

        entityManager.getTransaction().begin();
        entityManager.persist(createdApp);
        entityManager.persist(deletedApp);
        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final TypedQuery<AppDO> query = entityManager.createNamedQuery("apps.getById", AppDO.class)
                .setParameter("id", createdApp.getId());

        final List<AppDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdApp);
    }

    @Test
    void getByExternalId() {
        final TypedQuery<AppDO> query = entityManager.createNamedQuery("apps.getByExternalId", AppDO.class)
                .setParameter("externalId", createdApp.getExternalId());

        final List<AppDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdApp);
    }

    @Test
    void getByParentAccountId() {
        final TypedQuery<AppDO> query = entityManager.createNamedQuery("apps.getByAccountId", AppDO.class)
                .setParameter("parentAccountId", createdApp.getParentAccountId());

        final List<AppDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdApp);
    }

    @Test
    void getDeletedById() {
        final TypedQuery<AppDO> query = entityManager.createNamedQuery("apps.getById", AppDO.class)
                .setParameter("id", deletedApp.getId());

        final List<AppDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }
}
