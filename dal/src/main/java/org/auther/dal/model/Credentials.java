package org.auther.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface Credentials extends AbstractDO {
    String getAccountId();
    String getUsername();
    HashedPasswordDO getHashedPassword();

    interface Builder extends AbstractDO.Builder {}
}
