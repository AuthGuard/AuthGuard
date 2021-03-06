package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface HashedPassword {
    String getPassword();
    String getSalt();
}
