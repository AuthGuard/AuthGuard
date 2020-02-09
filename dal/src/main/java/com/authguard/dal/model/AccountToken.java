package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.ZonedDateTime;

@Value.Immutable
@DOStyle
@JsonSerialize(as = AccountTokenDO.class)
@JsonDeserialize(as = AccountTokenDO.class)
public interface AccountToken extends AbstractDO {
    String getToken();
    String getAssociatedAccountId();
    ZonedDateTime expiresAt();

    interface Builder extends AbstractDO.Builder {}
}
