package com.nexblocks.authguard.dal.model;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AccountTokenJpaTest {
    private EntityManager entityManager;
    private AccountTokenDO createdAccountToken;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(AccountTokenDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        createdAccountToken = AccountTokenDO.builder()
                .id("account-token-id")
                .token("test-token")
                .expiresAt(Instant.now())
                .associatedAccountId("account")
                .additionalInformation(ImmutableMap.of("key", "value"))
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(createdAccountToken);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final AccountTokenDO retrieved = entityManager.find(AccountTokenDO.class, createdAccountToken.getId());

        assertThat(retrieved).isEqualTo(createdAccountToken);
    }

    @Test
    void getByToken() {
        final TypedQuery<AccountTokenDO> query = entityManager.createNamedQuery("account_tokens.getByToken", AccountTokenDO.class)
                .setParameter("token", createdAccountToken.getToken());

        final List<AccountTokenDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdAccountToken);
    }
}
