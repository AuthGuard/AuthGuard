package com.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface RequestContext {
    String getIdempotentKey();
    String getSource();
}
