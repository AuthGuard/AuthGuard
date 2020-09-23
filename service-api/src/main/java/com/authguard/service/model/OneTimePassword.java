package com.authguard.service.model;

import org.immutables.value.Value;

import java.time.ZonedDateTime;

@Value.Immutable
@BOStyle
public interface OneTimePassword {
    String getId();
    String getAccountId();
    String getPassword();
    ZonedDateTime getExpiresAt();
}
