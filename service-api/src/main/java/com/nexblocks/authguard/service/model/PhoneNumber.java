package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface PhoneNumber {
    String getNumber();
    boolean isVerified();
}
