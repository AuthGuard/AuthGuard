package com.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface IdempotentRecord {
    String getIdempotentKey();
    String getEntityId();
    String getEntityType();
}
