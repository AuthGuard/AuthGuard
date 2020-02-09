package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.ZonedDateTime;

@Value.Immutable
@DOStyle
@JsonSerialize(as = OneTimePasswordDO.class)
@JsonDeserialize(as = OneTimePasswordDO.class)
public interface OneTimePassword extends AbstractDO {
    String getAccountId();
    String getPassword();
    ZonedDateTime getExpiresAt();

    interface Builder extends AbstractDO.Builder {}
}
