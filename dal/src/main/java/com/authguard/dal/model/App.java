package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
@JsonSerialize(as = AppDO.class)
@JsonDeserialize(as = AppDO.class)
public interface App extends AbstractDO {
    String getName();
    String getParentAccountId();
    List<String> getRoles();
    List<PermissionDO> getPermissions();
    List<String> getScopes();
    boolean isActive();

    interface Builder extends AbstractDO.Builder {}
}
