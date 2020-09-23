package com.authguard.service.model;

import org.immutables.value.Value;

import java.time.ZonedDateTime;

@Value.Immutable
@BOStyle
public interface Session {
    String getId();
    String getAccountId();
    ZonedDateTime getExpiresAt();
}
