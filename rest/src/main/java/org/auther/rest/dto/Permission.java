package org.auther.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = PermissionDTO.class)
@JsonDeserialize(as = PermissionDTO.class)
public interface Permission {
    String getGroup();
    String getName();
}
