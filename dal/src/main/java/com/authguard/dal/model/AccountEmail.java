package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DOStyle
@JsonDeserialize(as = AccountEmailDO.class)
@JsonSerialize(as = AccountEmailDO.class)
public interface AccountEmail {
    String getEmail();
    boolean isVerified();
    boolean isPrimary();
    boolean isBackup();
}
