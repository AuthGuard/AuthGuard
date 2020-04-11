package com.authguard.service.model;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@BOStyle
public interface Credentials {
    String getId();
    String getAccountId();
    List<UserIdentifierBO> getIdentifiers();
    String getPlainPassword();
    HashedPasswordBO getHashedPassword();
}
