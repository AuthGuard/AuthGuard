package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = CreatePermissionRequestDTO.class)
@JsonDeserialize(as = CreatePermissionRequestDTO.class)
public interface CreatePermissionRequest {
    String getGroup();
    String getName();
    String getDomain();
}
