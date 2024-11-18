package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;

@Value.Immutable
@BOStyle
public interface Session {
    String getDomain();
    long getId();
    String getSessionToken();
    long getAccountId();
    Instant getExpiresAt();
    boolean isForTracking();
    boolean isActive();
    Map<String, String> getData();
}
