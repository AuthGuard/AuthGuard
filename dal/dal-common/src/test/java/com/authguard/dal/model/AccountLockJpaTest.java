package com.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AccountLockJpaTest {
    private EntityManager entityManager;
    private AccountLockDO createdLock;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(AccountLockDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        createdLock = AccountLockDO.builder()
                .id("account-lock-id")
                .expiresAt(OffsetDateTime.now())
                .accountId("account")
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(createdLock);

        entityManager.getTransaction().commit();
    }

    @Test
    void getByAccount() {
        final TypedQuery<AccountLockDO> query = entityManager.createNamedQuery("account_locks.getByAccountId", AccountLockDO.class)
                .setParameter("accountId", createdLock.getAccountId());

        final List<AccountLockDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdLock);
    }
}
