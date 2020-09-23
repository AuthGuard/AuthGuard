package com.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Tokens {
    String getType();
    String getId();
    Object getToken();
    Object getRefreshToken();
}
