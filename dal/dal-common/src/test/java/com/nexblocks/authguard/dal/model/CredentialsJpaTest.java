package com.nexblocks.authguard.dal.model;

import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CredentialsJpaTest {
    private EntityManager entityManager;
    private CredentialsDO createdCredentials;
    private CredentialsDO deletedCredentials;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(CredentialsDO.class)
                .withMappedClass(UserIdentifierDO.class)
                .withMappedClass(PasswordDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create credentials
        createdCredentials = CredentialsDO.builder()
                .id(1)
                .accountId(101)
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

        deletedCredentials = CredentialsDO.builder()
                .id(2)
                .deleted(true)
                .accountId(101)
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

        entityManager.getTransaction().begin();

        entityManager.persist(createdCredentials);
        entityManager.persist(deletedCredentials);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getById", CredentialsDO.class)
                .setParameter("id", createdCredentials.getId());

        final List<CredentialsDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdCredentials);
    }

    @Test
    void getByAccountId() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getByAccountId", CredentialsDO.class)
                .setParameter("accountId", createdCredentials.getAccountId());

        final List<CredentialsDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdCredentials);
    }

    @Test
    void getByIdentifier() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getByIdentifier", CredentialsDO.class)
                .setParameter("identifier", "username")
                .setParameter("domain", "main");

        final List<CredentialsDO> retrieved = query.getResultList();
        assertThat(retrieved).containsExactly(createdCredentials);
    }

    @Test
    void getDeletedById() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getById", CredentialsDO.class)
                .setParameter("id", deletedCredentials.getId());

        final List<CredentialsDO> retrieved = query.getResultList();
        assertThat(retrieved).isEmpty();
    }

    @Test
    void getByNonexistentIdentifier() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getByIdentifier", CredentialsDO.class)
                .setParameter("identifier", "nonsense")
                .setParameter("domain", "main");

        final List<CredentialsDO> retrieved = query.getResultList();
        assertThat(retrieved).isEmpty();
    }

    @Test
    void createDuplicate() {
        final CredentialsDO duplicate = CredentialsDO.builder()
                .id(3)
                .accountId(101)
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
