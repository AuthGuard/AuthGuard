package org.auther.service;

import org.auther.service.model.BOStyle;
import org.immutables.value.Value;

@Value.Immutable
@BOStyle
public interface PermissionGroup {
    String getName();
}
