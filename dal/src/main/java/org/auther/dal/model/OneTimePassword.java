package org.auther.dal.model;

import org.immutables.value.Value;

import java.time.ZonedDateTime;

@Value.Immutable
@DOStyle
public interface OneTimePassword extends AbstractDO {
    String getAccountId();
    String getPassword();
    ZonedDateTime getExpiresAt();

    interface Builder extends AbstractDO.Builder {}
}
