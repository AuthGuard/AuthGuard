package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface AccountEmail {
    String getEmail();
    boolean isVerified();
    boolean isActive();
}
