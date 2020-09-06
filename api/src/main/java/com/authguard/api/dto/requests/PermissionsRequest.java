package com.authguard.api.dto.requests;

import com.authguard.api.dto.entities.PermissionDTO;
import com.authguard.api.dto.style.DTOStyle;
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
