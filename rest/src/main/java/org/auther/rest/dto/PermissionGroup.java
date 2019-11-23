package org.auther.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = PermissionGroupDTO.class)
@JsonDeserialize(as = PermissionGroupDTO.class)
public interface PermissionGroup {
    String getName();
}
