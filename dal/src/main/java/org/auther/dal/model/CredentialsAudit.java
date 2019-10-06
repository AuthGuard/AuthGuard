package org.auther.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface CredentialsAudit {
    String getId();
    Action getAction();
    String getCredentialId();
    String getUsername();
    HashedPasswordDO getPassword();

    enum Action {
        ATTEMPT,
        UPDATED
    }
}
