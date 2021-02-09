package com.nexblocks.authguard.api.dto.requests;

import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = PermissionsRequestDTO.class)
@JsonDeserialize(as = PermissionsRequestDTO.class)
public interface PermissionsRequest {
    Action getAction();
    List<PermissionDTO> getPermissions();

    enum Action {
        GRANT,
        REVOKE
    }
}
