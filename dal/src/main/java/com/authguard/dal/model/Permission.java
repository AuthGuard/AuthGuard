package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DOStyle
@JsonSerialize(as = PermissionDO.class)
@JsonDeserialize(as = PermissionDO.class)
public interface Permission extends AbstractDO {
    String getGroup();
    String getName();

    interface Builder extends AbstractDO.Builder {}
}
