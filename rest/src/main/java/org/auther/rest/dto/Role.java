package org.auther.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = RoleDTO.class)
@JsonSerialize(as = RoleDTO.class)
public interface Role {
    String getName();
    List<PermissionDTO> getPermissions();
}
