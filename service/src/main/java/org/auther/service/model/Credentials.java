package org.auther.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Credentials {
    String getId();
    String getAccountId();
    String getUsername();
    String getPlainPassword();
    HashedPasswordBO getHashedPassword();
}
