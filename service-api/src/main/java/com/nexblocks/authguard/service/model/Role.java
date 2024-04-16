package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Role extends Entity {
    String getName();
    boolean isForAccounts();
    boolean isForApplications();
}
