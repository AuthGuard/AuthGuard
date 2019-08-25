package org.auther.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface Credentials {
    String getId();
    String getAccountId();
    String getUsername();
    String getPlainPassword();
    HashedPasswordDO getHashedPassword();
}
