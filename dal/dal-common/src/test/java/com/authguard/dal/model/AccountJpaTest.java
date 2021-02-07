package com.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountJpaTest {
    private EntityManager entityManager;
    private AccountDO createdAccount;
    private AccountDO deletedAccount;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2().withMappedClass(AccountDO.class)
                .withMappedClass(PermissionDO.class)
                .withMappedClass(RoleDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create test permission
        entityManager.getTransaction().begin();

        entityManager.persist(PermissionDO.builder()
                .id("read-posts-permission")
                .group("posts")
                .name("read")
                .build());

        entityManager.getTransaction().commit();

        // create the test account
        entityManager.getTransaction().begin();

        createdAccount = AccountDO.builder()
                .id("test-account")
                .roles(Collections.singleton("test"))
                .externalId("test-account-external")
                .permissions(Collections.singleton(PermissionDO.builder()
                        .id("read-posts-permission")
                        .build()))
                .email(EmailDO.builder()
                        .email("primary@emails.com")
                        .build()
                ).build();

        deletedAccount = AccountDO.builder()
                .id("deleted-account")
                .deleted(true)
                .roles(Collections.singleton("test"))
                .externalId("test-account-external")
                .permissions(Collections.singleton(PermissionDO.builder()
                        .id("read-posts-permission")
                        .build()))
                .email(EmailDO.builder()
                        .email("deleted@emails.com")
                        .build()
                ).build();

        entityManager.persist(createdAccount);
        entityManager.persist(deletedAccount);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getById", AccountDO.class)
                .setParameter("id", createdAccount.getId());

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByExternalId() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByExternalId", AccountDO.class)
                .setParameter("externalId", createdAccount.getExternalId());

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByRole() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByRole", AccountDO.class)
                .setParameter("role", "test");

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByWrongRole() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByRole", AccountDO.class)
                .setParameter("role", "nonexistent");

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }

    @Test
    void getDeletedById() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getById", AccountDO.class)
                .setParameter("id", deletedAccount.getId());

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }
}