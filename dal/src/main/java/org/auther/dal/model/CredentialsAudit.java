package org.auther.dal.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@DOStyle
public interface CredentialsAudit {
    String getId();
    Action getAction();
    String getCredentialId();
    String getUsername();
    @Nullable HashedPasswordDO getPassword();

    enum Action {
        ATTEMPT,
        UPDATED
    }
}
