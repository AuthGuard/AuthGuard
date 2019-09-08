package org.auther.dal.model;

import org.immutables.value.Value;

@Value.Immutable
@DOStyle
public interface Permission {
    String getGroup();
    String getName();
}
