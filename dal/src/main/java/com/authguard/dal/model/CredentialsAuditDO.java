package com.authguard.dal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CredentialsAuditDO extends AbstractDO {
    private Action action;
    private String credentialsId;
    private UserIdentifierDO identifier;
    private PasswordDO password;

    public enum Action {
        ATTEMPT,
        UPDATED,
        DELETED
    }
}
