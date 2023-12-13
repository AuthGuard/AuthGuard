package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface ActionToken {
    String getToken();
    String getAction();
    Long getAccountId();
    long getValidFor();
}
