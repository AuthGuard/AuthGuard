package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.Instant;
import java.util.Map;

@Value.Immutable
@BOStyle
public interface Session {
    String getId();
    String getSessionToken();
    String getAccountId();
    Instant getExpiresAt();
    Map<String, String> getData();
}
