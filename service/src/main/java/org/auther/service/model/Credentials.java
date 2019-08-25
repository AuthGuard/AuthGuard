package org.auther.service.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@BOStyle
public interface Credentials {
    String getId();
    String getAccountId();
    String getUsername();
    @Nullable String getPlainPassword();
    @Nullable HashedPasswordBO getHashedPassword();
}
