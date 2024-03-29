package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@BOStyle
public interface ApiKey extends Entity {
    long getAppId();
    String getKey();
    String getType();
    String getName();
    boolean isForClient();
    Instant getExpiresAt();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "ApiKey";
    }
}
