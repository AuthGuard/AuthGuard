package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface IdempotentRecord {
    long getId();
    String getIdempotentKey();
    Long getEntityId();
    String getEntityType();
}
