package com.authguard.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = PermissionsRequestDTO.class)
@JsonDeserialize(as = PermissionsRequestDTO.class)
public interface PermissionsRequest {
    List<PermissionDTO> getPermissions();
}
