package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DOStyle
@JsonSerialize(as = CredentialsAuditDO.class)
@JsonDeserialize(as = CredentialsAuditDO.class)
public interface CredentialsAudit extends AbstractDO {
    Action getAction();
    String getCredentialId();
    String getUsername();
    HashedPasswordDO getPassword();

    enum Action {
        ATTEMPT,
        UPDATED
    }

    interface Builder extends AbstractDO.Builder {}
}
