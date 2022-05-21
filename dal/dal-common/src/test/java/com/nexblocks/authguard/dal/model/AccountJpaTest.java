package com.nexblocks.authguard.dal.model;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountJpaTest {
    private EntityManager entityManager;
    private AccountDO createdAccount;
    private AccountDO deletedAccount;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2().withMappedClass(AccountDO.class)
                .withMappedClass(UserIdentifierDO.class)
                .withMappedClass(PermissionDO.class)
                .withMappedClass(RoleDO.class)
                .withMappedClass(PasswordDO.class)
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
                .id("1")
                .roles(Collections.singleton("test"))
                .externalId("test-account-external")
                .permissions(Collections.singleton(PermissionDO.builder()
                        .id("read-posts-permission")
                        .group("posts")
                        .name("read")
                        .build()))
                .email(EmailDO.builder()
                        .email("primary@emails.com")
                        .build())
                .backupEmail(EmailDO.builder()
                        .email("backup@emails.com")
                        .build())
                .metadata(ImmutableMap.of("meta-1", "value-1"))
                .domain("main")
                .hashedPassword(PasswordDO.builder()
                        .password("password")
                        .salt("salt")
                        .build())
                .identifiers(Collections.singleton(UserIdentifierDO.builder()
                        .identifier("username")
                        .type(UserIdentifierDO.Type.USERNAME)
                        .domain("main")
                        .build()))
                .build();

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
                        .build())
                .domain("main")
                .hashedPassword(PasswordDO.builder()
                        .password("password")
                        .salt("salt")
                        .build())
                .identifiers(Collections.singleton(UserIdentifierDO.builder()
                        .identifier("deleted-username")
                        .type(UserIdentifierDO.Type.USERNAME)
                        .domain("main")
                        .build()))
                .build();

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
    void getByEmail() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByEmail", AccountDO.class)
                .setParameter("email", createdAccount.getEmail().getEmail())
                .setParameter("domain", "main");

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByBackupEmail() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByEmail", AccountDO.class)
                .setParameter("email", createdAccount.getBackupEmail().getEmail())
                .setParameter("domain", "main");

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByWrongEmail() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByEmail", AccountDO.class)
                .setParameter("email", "nonexistent")
                .setParameter("domain", "main");

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }

    @Test
    void insertDuplicateEmail() {

        entityManager.getTransaction().begin();

        entityManager.persist(AccountDO.builder()
                .id("not-to-be-inserted")
                .roles(Collections.singleton("test"))
                .permissions(Collections.singleton(PermissionDO.builder()
                        .id("read-posts-permission")
                        .build()))
                .email(EmailDO.builder()
                        .email("primary@emails.com")
                        .build())
                .domain("main")
                .build());

        Assertions.assertThatThrownBy(() -> entityManager.getTransaction().commit())
                .isInstanceOf(PersistenceException.class)
                .hasCauseInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void getByRole() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByRole", AccountDO.class)
                .setParameter("role", "test")
                .setParameter("domain", "main");

        final List<AccountDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByWrongRole() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByRole", AccountDO.class)
                .setParameter("role", "nonexistent")
                .setParameter("domain", "main");

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

    @Test
    void getByIdentifier() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByIdentifier", AccountDO.class)
                .setParameter("identifier", "username")
                .setParameter("domain", "main");

        final List<AccountDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdAccount);
    }

    @Test
    void getByNonexistentIdentifier() {
        final TypedQuery<AccountDO> query = entityManager.createNamedQuery("accounts.getByIdentifier", AccountDO.class)
                .setParameter("identifier", "nonsense")
                .setParameter("domain", "main");

        final List<AccountDO> retrieved = query.getResultList();
        assertThat(retrieved).isEmpty();
    }

    @Test
    void createDuplicate() {
        final AccountDO duplicate = AccountDO.builder()
                .id("duplicate-credentials")
                .hashedPassword(PasswordDO.builder()
                        .password("password")
                        .salt("salt")
                        .build())
                .identifiers(Collections.singleton(UserIdentifierDO.builder()
                        .identifier("username")
                        .type(UserIdentifierDO.Type.USERNAME)
                        .domain("main")
                        .build()))
                .build();

        entityManager.getTransaction().begin();

        try {
            entityManager.persist(duplicate);
            entityManager.getTransaction().commit();
        } catch (final PersistenceException persistenceException) {
            final Throwable cause = persistenceException.getCause();

            if (cause instanceof ConstraintViolationException) {
                assertThat(((ConstraintViolationException) cause).getConstraintName())
                        .contains("IDENTIFIER_DUP");
            } else {
                throw persistenceException;
            }
        } finally {
            entityManager.getTransaction().rollback();
        }
    }
}