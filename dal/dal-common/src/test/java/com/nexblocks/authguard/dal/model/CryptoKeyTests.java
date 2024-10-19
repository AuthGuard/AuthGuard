package com.nexblocks.authguard.dal.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CryptoKeyTests {
    private EntityManager entityManager;
    private CryptoKeyDO first;
    private CryptoKeyDO second;

    @BeforeAll
    public void setup() {
        final H2 h2 = new H2()
                .withMappedClass(CryptoKeyDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create credentials
        first = CryptoKeyDO.builder()
                .id(1)
                .domain("main")
                .accountId(101L)
                .appId(201L)
                .createdAt(Instant.now().minusMillis(1))
                .build();

        second = CryptoKeyDO.builder()
                .id(2)
                .domain("main")
                .accountId(101L)
                .appId(201L)
                .createdAt(Instant.now())
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(first);
        entityManager.persist(second);

        entityManager.getTransaction().commit();
    }

    @Test
    void getByAccountId() {
        TypedQuery<CryptoKeyDO> firstPageQuery = entityManager.createNamedQuery("crypto_keys.getByAccountId",
                        CryptoKeyDO.class)
                .setParameter("domain", first.getDomain())
                .setParameter("id", first.getAccountId())
                .setParameter("cursor", Instant.now().plusMillis(1))
                .setMaxResults(1);

        List<CryptoKeyDO> firstPage = firstPageQuery.getResultList();
        assertThat(firstPage).containsExactly(second);

        TypedQuery<CryptoKeyDO> secondPageQuery = entityManager.createNamedQuery("crypto_keys.getByAccountId",
                        CryptoKeyDO.class)
                .setParameter("domain", first.getDomain())
                .setParameter("id", first.getAccountId())
                .setParameter("cursor", second.getCreatedAt())
                .setMaxResults(1);

        List<CryptoKeyDO> secondPage = firstPageQuery.getResultList();
        assertThat(secondPage).containsExactly(first);
    }

    @Test
    void getByAppId() {
        TypedQuery<CryptoKeyDO> firstPageQuery = entityManager.createNamedQuery("crypto_keys.getByAppId",
                        CryptoKeyDO.class)
                .setParameter("domain", first.getDomain())
                .setParameter("id", first.getAppId())
                .setParameter("cursor", Instant.now().plusMillis(1))
                .setMaxResults(1);

        List<CryptoKeyDO> firstPage = firstPageQuery.getResultList();
        assertThat(firstPage).containsExactly(second);

        TypedQuery<CryptoKeyDO> secondPageQuery = entityManager.createNamedQuery("crypto_keys.getByAppId",
                        CryptoKeyDO.class)
                .setParameter("domain", first.getDomain())
                .setParameter("id", first.getAppId())
                .setParameter("cursor", second.getCreatedAt())
                .setMaxResults(1);

        List<CryptoKeyDO> secondPage = firstPageQuery.getResultList();
        assertThat(secondPage).containsExactly(first);
    }
}
