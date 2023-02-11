package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@BOStyle
public interface ApiKey extends Entity {
    String getAppId();
    String getKey();
    String getType();
    Instant getExpiresAt();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "ApiKey";
    }
}
