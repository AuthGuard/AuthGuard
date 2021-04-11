package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.OffsetDateTime;
import java.util.Map;

@Value.Immutable
@BOStyle
public interface Session {
    String getId();
    String getSessionToken();
    String getAccountId();
    OffsetDateTime getExpiresAt();
    Map<String, String> getData();
}
