package org.auther.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface PermissionGroup extends AbstractDO {
    String getName();

    interface Builder extends AbstractDO.Builder {}
}
