package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@BOStyle
public interface OneTimePassword {
    long getId();
    long getAccountId();
    String getPassword();
    Instant getExpiresAt();
    String getDeviceId();
    String getClientId();
    String getExternalSessionId();
    String getSourceIp();
    String getUserAgent();
}
