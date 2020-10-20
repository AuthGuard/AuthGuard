package com.authguard.dal;

import com.authguard.dal.model.AccountDO;
import com.authguard.dal.model.EmailDO;
import com.authguard.dal.model.PermissionDO;
import com.authguard.dal.model.RoleDO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountJpaTest {
    private EntityManager entityManager;
    private AccountDO createdAccount;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2().withMappedClass(AccountDO.class)
                .withMappedClass(EmailDO.class)
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
                .emails(new HashSet<>(Arrays.asList(
                        EmailDO.builder()
                                .email("get_by_id@emails.com")
                                .build(),
                        EmailDO.builder()
                                .email("generic@emails.com")
                                .build()
                )))
                .build();

        entityManager.persist(createdAccount);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final AccountDO retrieved = entityManager.find(AccountDO.class, createdAccount.getId());

        assertThat(retrieved).isEqualTo(createdAccount);
    }

    @Test
    void getByExternalId() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByExternalId", AccountDO.class)
                .setParameter("externalId", createdAccount.getExternalId());

        final List<AccountDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByRole() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByRole", AccountDO.class)
                .setParameter("role", "test");

        final List<AccountDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByWrongRole() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByRole", AccountDO.class)
                .setParameter("role", "nonexistent");

        final List<AccountDO> retrieved = query.getResultList();
        assertThat(retrieved).isEmpty();
    }
}