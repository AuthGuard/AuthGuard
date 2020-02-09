package com.authguard.dal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DOStyle
@JsonDeserialize(as = AccountDO.class)
@JsonSerialize(as = AccountDO.class)
public interface Account extends AbstractDO {
    List<String> getRoles();
    List<PermissionDO> getPermissions();
    List<String> getScopes();
    boolean isActive();

    interface Builder extends AbstractDO.Builder {}
}
