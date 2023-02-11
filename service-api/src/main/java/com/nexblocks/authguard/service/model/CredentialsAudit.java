package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface CredentialsAudit {
    String getId();
    String getCredentialsId();
    Action getAction();

    String getIdentifier();
    HashedPasswordBO getPassword();

    enum Action {
        ATTEMPT,
        UPDATED,
        DELETED,
        DEACTIVATED
    }
}
