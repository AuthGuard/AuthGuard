package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DOStyle
@JsonSerialize(as = HashedPasswordDO.class)
@JsonDeserialize(as = HashedPasswordDO.class)
public interface HashedPassword {
    String getPassword();
    String getSalt();
}
