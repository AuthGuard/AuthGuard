package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DOStyle
@JsonSerialize(as = ApiKeyDO.class)
@JsonDeserialize(as = ApiKeyDO.class)
public interface ApiKey extends AbstractDO {
    String getJti();
    String getKey();
    String getAppId();

    interface Builder extends AbstractDO.Builder {}
}
