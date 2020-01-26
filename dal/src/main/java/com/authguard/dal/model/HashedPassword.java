package com.authguard.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface HashedPassword {
    String getPassword();
    String getSalt();
}
