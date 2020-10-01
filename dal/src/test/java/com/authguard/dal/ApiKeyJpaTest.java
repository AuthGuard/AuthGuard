package com.authguard.dal;

import com.authguard.dal.model.ApiKeyDO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiKeyJpaTest {
    private EntityManager entityManager;
    private ApiKeyDO createdApiKey;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(ApiKeyDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        createdApiKey = ApiKeyDO.builder()
                .id("api-key-id")
                .appId("app-id")
                .key("key")
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(createdApiKey);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final ApiKeyDO retrieved = entityManager.find(ApiKeyDO.class, createdApiKey.getId());

        assertThat(retrieved).isEqualTo(createdApiKey);
    }

    @Test
    void getByKey() {
        final TypedQuery<ApiKeyDO> query = entityManager.createNamedQuery("api_keys.getByKey", ApiKeyDO.class)
                .setParameter("key", createdApiKey.getKey());

        final List<ApiKeyDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdApiKey);
    }
}
