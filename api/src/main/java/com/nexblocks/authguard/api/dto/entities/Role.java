package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = RoleDTO.class)
@JsonSerialize(as = RoleDTO.class)
public interface Role {
    String getId();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getLastModified();
    String getName();
}
