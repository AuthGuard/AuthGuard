package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@BOStyle
public interface AccountLock {
    Long getAccountId();
    Instant getExpiresAt();
}
