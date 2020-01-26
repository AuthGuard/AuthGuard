package com.authguard.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
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
