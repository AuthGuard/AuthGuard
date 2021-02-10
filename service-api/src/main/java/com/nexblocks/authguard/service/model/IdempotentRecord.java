package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface IdempotentRecord {
    String getId();
    String getIdempotentKey();
    String getEntityId();
    String getEntityType();
}
