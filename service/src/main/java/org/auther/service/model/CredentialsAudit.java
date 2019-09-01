package org.auther.service.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@BOStyle
public interface CredentialsAudit {
    String getId();
    String getCredentialId();
    Action getAction();
    String getUsername();
    @Nullable HashedPasswordBO getPassword();

    enum Action {
        ATTEMPT,
        UPDATED
    }
}
