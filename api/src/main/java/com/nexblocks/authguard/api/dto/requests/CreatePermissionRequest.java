package com.nexblocks.authguard.api.dto.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nexblocks.authguard.api.dto.entities.DomainScoped;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonSerialize(as = CreatePermissionRequestDTO.class)
@JsonDeserialize(as = CreatePermissionRequestDTO.class)
public interface CreatePermissionRequest extends DomainScoped {
    String getGroup();
    String getName();
}
