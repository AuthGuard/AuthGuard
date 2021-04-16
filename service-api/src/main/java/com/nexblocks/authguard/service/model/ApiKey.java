package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface ApiKey extends Entity {
    String getAppId();
    String getKey();

    @Override
    @Value.Derived
    default String getEntityType() {
        return "ApiKey";
    }
}
