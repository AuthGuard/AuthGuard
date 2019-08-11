package org.auther.api.dto;

import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
public interface Permission {
    String getGroup();
    String getName();
}
