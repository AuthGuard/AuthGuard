package com.nexblocks.authguard.dal.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CredentialsAuditJpaTest {
    private EntityManager entityManager;
    private CredentialsAuditDO created;

    @BeforeAll
    void setup() {
        final H2 h2 = new H2()
                .withMappedClass(CredentialsAuditDO.class)
                .prepare();

        entityManager = h2.getEntityManager();

        // create audit
        created = CredentialsAuditDO.builder()
                .id(1)
                .credentialsId(101)
                .action(CredentialsAuditDO.Action.UPDATED)
                .password(PasswordDO.builder()
                        .password("password")
                        .salt("sal")
                        .build())
                .build();

        entityManager.getTransaction().begin();

        entityManager.persist(created);

        entityManager.getTransaction().commit();
    }

    @Test
    void getById() {
        final CredentialsAuditDO retrieved = entityManager.find(CredentialsAuditDO.class, created.getId());

        assertThat(retrieved).isEqualTo(created);
    }

    @Test
    void getByCredentialsId() {
        final TypedQuery<CredentialsAuditDO> query = entityManager.createNamedQuery("credentials_audit.getByCredentialsId", CredentialsAuditDO.class)
                .setParameter("credentialsId", created.getCredentialsId());

        final List<CredentialsAuditDO> retrieved = query.getResultList();
        Assertions.assertThat(retrieved).containsExactly(created);
    }
}
