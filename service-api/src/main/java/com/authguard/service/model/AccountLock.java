package com.authguard.service.model;

import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@BOStyle
public interface AccountLock {
    String getAccountId();
    OffsetDateTime getExpiresAt();
}
