package com.authguard.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface UserIdentifier {
    Type getType();
    String getIdentifier();

    enum Type {
        USERNAME,
        EMAIL,
        PHONE_NUMBER
    }
}
