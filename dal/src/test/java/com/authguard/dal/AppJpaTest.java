package com.authguard.dal;

import com.authguard.dal.model.AppDO;
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
public class AppJpaTest {
    private EntityManager entityManager;
    private AppDO createdApp;

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
                .id("test-app")
                .parentAccountId("parent-account")
                .name("Test Application")
                .externalId("external-id")
                .build();

        entityManager.getTransaction().begin();
        entityManager.persist(createdApp);
        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final AppDO retrieved = entityManager.find(AppDO.class, createdApp.getId());

        assertThat(retrieved).isEqualTo(createdApp);
    }

    @Test
    void getByExternalId() {
        final TypedQuery<AppDO> query = entityManager.createNamedQuery("apps.getByExternalId", AppDO.class)
                .setParameter("externalId", createdApp.getExternalId());

        final List<AppDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdApp);
    }

    @Test
    void getByParentAccountId() {
        final TypedQuery<AppDO> query = entityManager.createNamedQuery("apps.getByAccountId", AppDO.class)
                .setParameter("parentAccountId", createdApp.getParentAccountId());

        final List<AppDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdApp);
    }
}
