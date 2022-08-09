package com.nexblocks.authguard.api.dto.entities;

import com.nexblocks.authguard.api.dto.style.DTOStyle;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@DTOStyle
@JsonDeserialize(as = RoleDTO.class)
@JsonSerialize(as = RoleDTO.class)
public interface Role {
    String getId();
    Instant getCreatedAt();
    Instant getLastModified();
    String getName();
    String getDomain();
}
