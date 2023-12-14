package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiKeyJpaTest {
    private EntityManager entityManager;
    private ApiKeyDO createdApiKey;
    private ApiKeyDO deletedAPiKey;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(ApiKeyDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        createdApiKey = ApiKeyDO.builder()
                .id(1)
                .appId(101)
                .key("key")
                .build();

        deletedAPiKey = ApiKeyDO.builder()
                .id(2)
                .deleted(true)
                .appId(101)
                .key("deleted-key")
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(createdApiKey);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final TypedQuery<ApiKeyDO> query = entityManager.createNamedQuery("api_keys.getById", ApiKeyDO.class)
                .setParameter("id", createdApiKey.getId());

        final List<ApiKeyDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdApiKey);
    }

    @Test
    void getByKey() {
        final TypedQuery<ApiKeyDO> query = entityManager.createNamedQuery("api_keys.getByKey", ApiKeyDO.class)
                .setParameter("key", createdApiKey.getKey());

        final List<ApiKeyDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdApiKey);
    }

    @Test
    void getByAppId() {
        final TypedQuery<ApiKeyDO> query = entityManager.createNamedQuery("api_keys.getByAppId", ApiKeyDO.class)
                .setParameter("appId", createdApiKey.getAppId());

        final List<ApiKeyDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdApiKey);
    }

    @Test
    void getDeletedById() {
        final TypedQuery<ApiKeyDO> query = entityManager.createNamedQuery("api_keys.getById", ApiKeyDO.class)
                .setParameter("id", deletedAPiKey.getId());

        final List<ApiKeyDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }
}
