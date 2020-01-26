package com.authguard.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface Permission extends AbstractDO {
    String getGroup();
    String getName();

    interface Builder extends AbstractDO.Builder {}
}
