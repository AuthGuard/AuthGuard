package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

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
                .id("created-credentials")
                .accountId("account")
                .hashedPassword(PasswordDO.builder()
                        .password("password")
                        .salt("salt")
                        .build())
                .identifiers(Collections.singleton(UserIdentifierDO.builder()
                        .identifier("username")
                        .type(UserIdentifierDO.Type.USERNAME)
                        .build()))
                .build();

        deletedCredentials = CredentialsDO.builder()
                .id("deleted-credentials")
                .deleted(true)
                .accountId("account")
                .hashedPassword(PasswordDO.builder()
                        .password("password")
                        .salt("salt")
                        .build())
                .identifiers(Collections.singleton(UserIdentifierDO.builder()
                        .identifier("deleted-username")
                        .type(UserIdentifierDO.Type.USERNAME)
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
        Assertions.assertThat(retrieved).containsExactly(createdCredentials);
    }

    @Test
    void getByAccountId() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getByAccountId", CredentialsDO.class)
                .setParameter("accountId", createdCredentials.getAccountId());

        final List<CredentialsDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdCredentials);
    }

    @Test
    void getByIdentifier() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getByIdentifier", CredentialsDO.class)
                .setParameter("identifier", "username");

        final List<CredentialsDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(createdCredentials);
    }

    @Test
    void getDeletedById() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getById", CredentialsDO.class)
                .setParameter("id", deletedCredentials.getId());

        final List<CredentialsDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }

    @Test
    void getByNonexistentIdentifier() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getByIdentifier", CredentialsDO.class)
                .setParameter("identifier", "nonsense");

        final List<CredentialsDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).isEmpty();
    }
}
