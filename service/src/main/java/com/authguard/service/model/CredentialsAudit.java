package com.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface CredentialsAudit {
    String getId();
    String getCredentialId();
    Action getAction();
    String getUsername();
    HashedPasswordBO getPassword();

    enum Action {
        ATTEMPT,
        UPDATED
    }
}
