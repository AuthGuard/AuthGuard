package com.authguard.service.model;

import org.immutables.value.Value;

import java.time.ZonedDateTime;
import java.util.Map;

@Value.Immutable
@BOStyle
public interface Session {
    String getId();
    String getSessionToken();
    String getAccountId();
    ZonedDateTime getExpiresAt();
    Map<String, String> getData();
}
