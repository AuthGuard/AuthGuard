package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DOStyle
@JsonSerialize(as = CredentialsDO.class)
@JsonDeserialize(as = CredentialsDO.class)
public interface Credentials extends AbstractDO {
    String getAccountId();
    String getUsername();
    HashedPasswordDO getHashedPassword();

    interface Builder extends AbstractDO.Builder {}
}
