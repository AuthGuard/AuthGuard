package org.auther.service.model;

import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface Permission {
    String getGroup();
    String getName();
}
