package com.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Tokens {
    EntityType getEntityType();
    String getEntityId();
    String getType();
    String getId();
    Object getToken();
    Object getRefreshToken();
}
