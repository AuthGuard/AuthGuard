package com.nexblocks.authguard.api.dto.requests;

import com.nexblocks.authguard.api.dto.entities.DomainScoped;
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
public interface CreateAppRequest extends DomainScoped {
    String getExternalId();
    String getName();
    String getAccountId();
    List<PermissionDTO> getPermissions();
    List<String> getScopes();
    List<String> getRoles();

    @Value.Default
    default boolean isActive() { return true; }
}
