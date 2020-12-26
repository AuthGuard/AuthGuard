package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

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
    private String credentialsId;

    @Embedded
    private PasswordDO password;

    public enum Action {
        ATTEMPT,
        UPDATED,
        DELETED
    }
}
