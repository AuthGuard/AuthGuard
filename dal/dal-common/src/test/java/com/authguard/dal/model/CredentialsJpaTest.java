package com.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CredentialsJpaTest {
    private EntityManager entityManager;
    private CredentialsDO created;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(CredentialsDO.class)
                .withMappedClass(UserIdentifierDO.class)
                .withMappedClass(PasswordDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create credentials
        created = CredentialsDO.builder()
                .id("credentials")
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

        entityManager.getTransaction().begin();

        entityManager.persist(created);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final CredentialsDO retrieved = entityManager.find(CredentialsDO.class, created.getId());

        assertThat(retrieved).isEqualTo(created);
    }

    @Test
    void getByAccountId() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getByAccountId", CredentialsDO.class)
                .setParameter("accountId", created.getAccountId());

        final List<CredentialsDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(created);
    }

    @Test
    void getByIdentifier() {
        final TypedQuery<CredentialsDO> query = entityManager.createNamedQuery("credentials.getByIdentifier", CredentialsDO.class)
                .setParameter("identifier", "username");

        final List<CredentialsDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(created);
    }
}
