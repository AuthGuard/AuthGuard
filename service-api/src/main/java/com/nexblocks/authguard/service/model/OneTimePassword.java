package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@BOStyle
public interface OneTimePassword {
    String getId();
    String getAccountId();
    String getPassword();
    OffsetDateTime getExpiresAt();
}
