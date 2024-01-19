package com.nexblocks.authguard.api.dto.requests;

import com.nexblocks.authguard.api.dto.entities.DomainScoped;
import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = CreateRoleRequestDTO.class)
@JsonSerialize(as = CreateRoleRequestDTO.class)
public interface CreateRoleRequest extends DomainScoped {
    String getName();
}
