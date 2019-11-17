package org.auther.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface AccountToken extends AbstractDO {
    String getToken();
    String getAssociatedAccountId();

    interface Builder extends AbstractDO.Builder {}
}
