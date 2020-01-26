package com.authguard.dal.model;

import org.immutables.value.Value;

import java.time.ZonedDateTime;

@Value.Immutable
@DOStyle
public interface AccountToken extends AbstractDO {
    String getToken();
    String getAssociatedAccountId();
    ZonedDateTime expiresAt();

    interface Builder extends AbstractDO.Builder {}
}
