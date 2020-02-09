package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
@JsonSerialize(as = RoleDO.class)
@JsonDeserialize(as = RoleDO.class)
public interface Role extends AbstractDO {
    String getName();
    List<PermissionDO> getPermissions();

    interface Builder extends AbstractDO.Builder {}
}
