package com.nexblocks.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
// JPA
@Entity
@Table(name = "credentials_audit")
@NamedQuery(
        name = "credentials_audit.getByCredentialsId",
        query = "SELECT credentials_audit FROM CredentialsAuditDO credentials_audit WHERE credentials_audit.credentialsId = :credentialsId"
)
public class CredentialsAuditDO extends AbstractDO {
    private Action action;
    private long credentialsId;

    private String identifier;

    @Embedded
    private PasswordDO password;

    public enum Action {
        ATTEMPT,
        UPDATED,
        DELETED,
        DEACTIVATED
    }
}
