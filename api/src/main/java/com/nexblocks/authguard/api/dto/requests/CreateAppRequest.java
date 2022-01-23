package com.nexblocks.authguard.api.dto.requests;

import com.nexblocks.authguard.api.dto.entities.PermissionDTO;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CreateAppRequestDTO.class)
@JsonSerialize(as = CreateAppRequestDTO.class)
public interface CreateAppRequest {
    String getExternalId();
    String getName();
    String getAccountId();
    String getDomain();
    List<PermissionDTO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();
    boolean isActive();
}
