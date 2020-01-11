package org.auther.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Tokens {
    String getType();
    String getId();
    String getToken();
    String getRefreshToken();
}
